package com.nexplm.platform.lifecycle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.lifecycle.domain.MetaLifecycle;
import com.nexplm.platform.lifecycle.mapper.MetaLifecycleMapper;
import com.nexplm.platform.metadata.domain.MetaEntity;
import com.nexplm.platform.metadata.mapper.MetaEntityMapper;
import com.nexplm.platform.object.domain.PlmObject;
import com.nexplm.platform.object.event.ObjectStateChangedEvent;
import com.nexplm.platform.object.mapper.PlmObjectMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 生命周期状态机：对象业务状态的<b>唯一写入口</b>。
 * <p>业务代码不得直接 UPDATE lifecycle_state，必须经过 {@link #transition}，
 * 以保证状态合法性与状态日志完整。</p>
 */
@Service
public class LifecycleService {

    public static final String DEFAULT_CODE = "DEFAULT_LIFECYCLE";

    public record StateDef(String code, String name, Boolean allFlow) {
    }

    public record TransitionDef(String from, String to, String action, String label) {
    }

    public record Definition(String code, String name, String initial, List<StateDef> states,
                             List<TransitionDef> transitions) {
    }

    private final MetaLifecycleMapper lifecycleMapper;
    private final MetaEntityMapper entityMapper;
    private final PlmObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper jsonMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final Map<String, Definition> cache = new ConcurrentHashMap<>();

    public LifecycleService(MetaLifecycleMapper lifecycleMapper, MetaEntityMapper entityMapper,
                            PlmObjectMapper objectMapper, JdbcTemplate jdbcTemplate,
                            ObjectMapper jsonMapper, ApplicationEventPublisher eventPublisher) {
        this.lifecycleMapper = lifecycleMapper;
        this.entityMapper = entityMapper;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.jsonMapper = jsonMapper;
        this.eventPublisher = eventPublisher;
    }

    public List<Definition> listDefinitions() {
        return lifecycleMapper.selectList(new LambdaQueryWrapper<MetaLifecycle>()
                        .orderByAsc(MetaLifecycle::getCode))
                .stream().map(this::parse).toList();
    }

    public Definition definition(String code) {
        return cache.computeIfAbsent(code, key -> {
            MetaLifecycle lifecycle = lifecycleMapper.selectOne(new LambdaQueryWrapper<MetaLifecycle>()
                    .eq(MetaLifecycle::getCode, key).last("limit 1"));
            if (lifecycle == null) {
                throw BizException.of(ErrorCode.COMMON_NOT_FOUND, "生命周期不存在：" + key);
            }
            return parse(lifecycle);
        });
    }

    public void invalidate(String code) {
        if (code == null) {
            cache.clear();
        } else {
            cache.remove(code);
        }
    }

    private Definition parse(MetaLifecycle lifecycle) {
        try {
            JsonNode root = jsonMapper.readTree(lifecycle.getDefinitionJson());
            List<StateDef> states = new ArrayList<>();
            for (JsonNode state : root.path("states")) {
                states.add(new StateDef(state.path("code").asText(), state.path("name").asText(),
                        state.path("allFlow").asBoolean(false)));
            }
            List<TransitionDef> transitions = new ArrayList<>();
            for (JsonNode transition : root.path("transitions")) {
                transitions.add(new TransitionDef(transition.path("from").asText(),
                        transition.path("to").asText(), transition.path("action").asText(),
                        transition.path("label").asText()));
            }
            return new Definition(lifecycle.getCode(), lifecycle.getName(),
                    root.path("initial").asText(states.isEmpty() ? null : states.get(0).code()),
                    List.copyOf(states), List.copyOf(transitions));
        } catch (Exception ex) {
            throw new IllegalStateException("生命周期定义解析失败：" + lifecycle.getCode(), ex);
        }
    }

    /**
     * 按对象类型解析其生命周期定义：类型上配置的优先，否则用默认生命周期。
     */
    public Definition definitionForType(String objectType) {
        String code = DEFAULT_CODE;
        MetaEntity entity = entityMapper.selectOne(new LambdaQueryWrapper<MetaEntity>()
                .eq(MetaEntity::getCode, objectType).last("limit 1"));
        if (entity != null && entity.getLifecycleCode() != null && !entity.getLifecycleCode().isBlank()) {
            code = entity.getLifecycleCode();
        }
        return definition(code);
    }

    /**
     * 预判某对象类型的 {@code fromState} 是否允许执行 {@code action}。
     *
     * <p>供批量业务（如变更执行）在事务内先行判定：非法迁移按「跳过」处理，而不是抛异常
     * 把整批回滚。真正写状态仍必须走 {@link #transition}，这里只读不写。</p>
     */
    public boolean canTransition(String objectType, String fromState, String action) {
        if (fromState == null || action == null) {
            return false;
        }
        return definitionForType(objectType).transitions().stream()
                .anyMatch(item -> item.from().equals(fromState) && item.action().equalsIgnoreCase(action));
    }

    /**
     * 保存生命周期定义（可视化设计器调用）。校验状态唯一、初始态存在、迁移端点合法。
     */
    @Transactional
    public void updateDefinition(String code, String definitionJson) {
        MetaLifecycle lifecycle = lifecycleMapper.selectOne(new LambdaQueryWrapper<MetaLifecycle>()
                .eq(MetaLifecycle::getCode, code).last("limit 1"));
        if (lifecycle == null) {
            throw BizException.of(ErrorCode.COMMON_NOT_FOUND, "生命周期不存在：" + code);
        }
        List<String> errors = new ArrayList<>();
        try {
            JsonNode root = jsonMapper.readTree(definitionJson);
            JsonNode states = root.path("states");
            if (!states.isArray() || states.isEmpty()) {
                errors.add("至少需要定义一个状态");
            }
            List<String> codes = new ArrayList<>();
            for (JsonNode state : states) {
                String stateCode = state.path("code").asText();
                if (stateCode.isBlank()) {
                    errors.add("状态编码不能为空");
                } else if (codes.contains(stateCode)) {
                    errors.add("状态编码重复：" + stateCode);
                } else {
                    codes.add(stateCode);
                }
            }
            String initial = root.path("initial").asText();
            if (!initial.isBlank() && !codes.contains(initial)) {
                errors.add("初始状态不在状态列表中：" + initial);
            }
            for (JsonNode transition : root.path("transitions")) {
                String from = transition.path("from").asText();
                String to = transition.path("to").asText();
                String action = transition.path("action").asText();
                if (!codes.contains(from)) {
                    errors.add("迁移起点不存在：" + from);
                }
                if (!codes.contains(to)) {
                    errors.add("迁移终点不存在：" + to);
                }
                if (action.isBlank()) {
                    errors.add("迁移操作不能为空");
                }
            }
        } catch (Exception ex) {
            errors.add("定义不是合法 JSON：" + ex.getMessage());
        }
        if (!errors.isEmpty()) {
            throw BizException.of(ErrorCode.META_PUBLISH_BLOCKED, String.join("；", errors));
        }
        lifecycle.setDefinitionJson(definitionJson);
        lifecycle.setVersionNo(lifecycle.getVersionNo() == null ? 1 : lifecycle.getVersionNo() + 1);
        lifecycle.setUpdatedAt(OffsetDateTime.now());
        lifecycleMapper.updateById(lifecycle);
        invalidate(code);
    }

    @Transactional
    public PlmObject transition(Long objectId, String action, String reason, Long operatorId) {
        PlmObject object = objectMapper.selectById(objectId);
        if (object == null) {
            throw BizException.of(ErrorCode.OBJ_NOT_FOUND);
        }
        Definition definition = definitionForType(object.getObjectType());
        String currentState = object.getLifecycleState();
        TransitionDef transition = definition.transitions().stream()
                .filter(item -> item.from().equals(currentState) && item.action().equalsIgnoreCase(action))
                .findFirst()
                .orElseThrow(() -> BizException.of(ErrorCode.OBJ_STATE_TRANSITION_DENIED,
                        "状态「" + stateName(definition, currentState) + "」不允许执行操作：" + action));

        PlmObject update = new PlmObject();
        update.setId(object.getId());
        update.setVersion(object.getVersion());
        update.setLifecycleState(transition.to());
        update.setUpdatedBy(operatorId);
        update.setUpdatedAt(OffsetDateTime.now());
        if ("Released".equals(transition.to())) {
            update.setReleasedAt(OffsetDateTime.now());
        }
        if ("Obsolete".equals(transition.to())) {
            update.setObsoleteAt(OffsetDateTime.now());
        }
        int rows = objectMapper.updateById(update);
        if (rows == 0) {
            throw BizException.of(ErrorCode.OBJ_VERSION_CONFLICT);
        }
        jdbcTemplate.update("""
                        INSERT INTO plm_state_log
                          (id, object_id, state_type, from_state, to_state, action, reason, operator_id, created_at)
                        VALUES (?, ?, 'MAIN', ?, ?, ?, ?, ?, ?)
                        """,
                IdWorker.getId(), object.getId(), currentState, transition.to(), action,
                reason == null ? null : reason.substring(0, Math.min(reason.length(), 500)),
                operatorId, OffsetDateTime.now());
        // 状态已落库且日志已写，才对外发布事件（监听方用 AFTER_COMMIT 接收，回滚不会外发）
        eventPublisher.publishEvent(new ObjectStateChangedEvent(object.getId(),
                object.getObjectType(), currentState, transition.to(), action,
                object.getCompanyId(), operatorId, OffsetDateTime.now()));
        object.setLifecycleState(transition.to());
        object.setVersion(object.getVersion() == null ? 0 : object.getVersion() + 1);
        return object;
    }

    private String stateName(Definition definition, String stateCode) {
        return definition.states().stream()
                .filter(state -> state.code().equals(stateCode))
                .map(StateDef::name)
                .findFirst()
                .orElse(stateCode);
    }
}
