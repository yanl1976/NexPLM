package com.nexplm.platform.doc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.doc.api.DocQueryDtos;
import com.nexplm.platform.doc.domain.DocSavedQuery;
import com.nexplm.platform.doc.mapper.DocSavedQueryMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 高级查询条件：保存/复用检索条件（个人 + 共享），条件以 JSON 原样保存，由前端解释执行。
 */
@Service
public class DocQueryService {

    private static final String ENTITY_CODE = "PdmDoc";

    private final DocSavedQueryMapper queryMapper;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper jsonMapper;

    public DocQueryService(DocSavedQueryMapper queryMapper, JdbcTemplate jdbcTemplate,
                           ObjectMapper jsonMapper) {
        this.queryMapper = queryMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.jsonMapper = jsonMapper;
    }

    /** 我的条件 + 他人共享的条件。 */
    public List<DocQueryDtos.SavedQuery> list(Long userId) {
        List<DocSavedQuery> rows = queryMapper.selectList(new LambdaQueryWrapper<DocSavedQuery>()
                .eq(DocSavedQuery::getEntityCode, ENTITY_CODE)
                .and(w -> w.eq(DocSavedQuery::getOwnerId, userId).or().eq(DocSavedQuery::getShared, true))
                .orderByDesc(DocSavedQuery::getUpdatedAt));
        Map<Long, String> names = new HashMap<>();
        List<Long> ownerIds = rows.stream().map(DocSavedQuery::getOwnerId).distinct().toList();
        if (!ownerIds.isEmpty()) {
            StringBuilder placeholders = new StringBuilder();
            for (int i = 0; i < ownerIds.size(); i++) {
                placeholders.append(i == 0 ? "?" : ",?");
            }
            jdbcTemplate.query("SELECT id, display_name FROM sys_user WHERE id IN (" + placeholders + ")",
                    rs -> {
                        names.put(rs.getLong(1), rs.getString(2));
                    }, ownerIds.toArray());
        }
        return rows.stream()
                .map(row -> new DocQueryDtos.SavedQuery(row.getId(), row.getName(), row.getEntityCode(),
                        row.getOwnerId(), names.get(row.getOwnerId()), row.getConditionJson(),
                        row.getShared(), userId.equals(row.getOwnerId()),
                        row.getCreatedAt(), row.getUpdatedAt()))
                .toList();
    }

    @Transactional
    public DocQueryDtos.SavedQuery create(DocQueryDtos.SavedQueryReq req, Long userId) {
        validateJson(req.conditionJson());
        OffsetDateTime now = OffsetDateTime.now();
        DocSavedQuery query = new DocSavedQuery();
        query.setId(IdWorker.getId());
        query.setName(req.name().trim());
        query.setEntityCode(ENTITY_CODE);
        query.setOwnerId(userId);
        query.setConditionJson(req.conditionJson());
        query.setShared(Boolean.TRUE.equals(req.shared()));
        query.setDeleted(false);
        query.setCreatedBy(userId);
        query.setCreatedAt(now);
        query.setUpdatedBy(userId);
        query.setUpdatedAt(now);
        queryMapper.insert(query);
        return toDetail(query, userId);
    }

    @Transactional
    public DocQueryDtos.SavedQuery update(Long id, DocQueryDtos.SavedQueryReq req, Long userId) {
        DocSavedQuery existing = requireOwned(id, userId);
        validateJson(req.conditionJson());
        existing.setName(req.name().trim());
        existing.setConditionJson(req.conditionJson());
        existing.setShared(Boolean.TRUE.equals(req.shared()));
        existing.setUpdatedBy(userId);
        existing.setUpdatedAt(OffsetDateTime.now());
        queryMapper.updateById(existing);
        return toDetail(existing, userId);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        requireOwned(id, userId);
        queryMapper.deleteById(id);
    }

    private DocSavedQuery requireOwned(Long id, Long userId) {
        DocSavedQuery query = queryMapper.selectById(id);
        if (query == null || !ENTITY_CODE.equals(query.getEntityCode())) {
            throw BizException.of(ErrorCode.COMMON_NOT_FOUND, "保存的查询不存在：" + id);
        }
        if (!query.getOwnerId().equals(userId)) {
            throw BizException.of(ErrorCode.COMMON_FORBIDDEN, "只能修改自己保存的查询条件");
        }
        return query;
    }

    private void validateJson(String conditionJson) {
        try {
            jsonMapper.readTree(conditionJson);
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "查询条件必须是合法 JSON：" + ex.getMessage());
        }
    }

    private DocQueryDtos.SavedQuery toDetail(DocSavedQuery query, Long userId) {
        return new DocQueryDtos.SavedQuery(query.getId(), query.getName(), query.getEntityCode(),
                query.getOwnerId(), null, query.getConditionJson(), query.getShared(),
                userId.equals(query.getOwnerId()), query.getCreatedAt(), query.getUpdatedAt());
    }
}
