package com.nexplm.platform.workflow.service.plugin;

import com.nexplm.platform.integration.api.IntegrationDtos;
import com.nexplm.platform.integration.kingdee.KingdeePayloadService;
import com.nexplm.platform.integration.service.IntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ErpPublish 插件：流程归档后把对象下发 ERP（金蝶适配器 P9-2 + 集成任务 P9-1）。
 *
 * <p>触发条件（全部满足才发，任一不满足只记日志不报错）：</p>
 * <ol>
 *   <li>流程事件是 {@link WfPluginContext#ACTION_COMPLETED}（走完全部分支）；</li>
 *   <li>对象类型在支持清单内（零部件 → {@code Material}，文档 → {@code Doc}）；</li>
 *   <li>对象生命周期已是 {@code Released}——审批通过但状态没推进（如自定义流程）不自动下发，
 *       避免把草稿/未发布版本推给 ERP；</li>
 *   <li>公司内存在启用的金蝶端点（{@code itg_endpoint.system_code = 'KINGDEE'}）；
 *       若配置了 {@code nexplm.workflow.erp-publish.endpoint-code}，则只认该 code 对应的端点
 *       （多账套：测试/生产），配了却找不到就跳过下发，不静默换账套。</li>
 * </ol>
 *
 * <p><b>为什么不直接 HTTP</b>：走 {@code IntegrationService.submitJob} 才能拿到
 * 「报文快照 + 幂等键 + 重试 + 死信」，事后可回答"当初发了什么"；幂等键带版本
 * （{@code PN-000001@R1}），同一版本重复归档不会二次下发。</p>
 */
@Component
@Order(200)
public class ErpPublishPlugin implements WfBusinessPlugin {

    private static final Logger log = LoggerFactory.getLogger(ErpPublishPlugin.class);

    /** 本地对象类型 → 金蝶载荷业务类型（与 {@code KingdeePayloadService} 支持清单对齐）。 */
    private static final Map<String, String> BIZ_TYPES = Map.of(
            "PdmItem", "Material",
            "PdmDoc", "Doc");

    private static final Set<String> SUPPORTED = BIZ_TYPES.keySet();

    private final JdbcTemplate jdbcTemplate;
    private final KingdeePayloadService payloadService;
    private final IntegrationService integrationService;
    private final boolean enabled;
    private final boolean dryRun;
    /** 指定下发到哪个金蝶账套（itg_endpoint.code）；留空沿用"id 最小的启用端点"。 */
    private final String endpointCode;

    public ErpPublishPlugin(JdbcTemplate jdbcTemplate,
                            KingdeePayloadService payloadService,
                            IntegrationService integrationService,
                            @Value("${nexplm.workflow.erp-publish.enabled:true}") boolean enabled,
                            @Value("${nexplm.workflow.erp-publish.dry-run:false}") boolean dryRun,
                            @Value("${nexplm.workflow.erp-publish.endpoint-code:}") String endpointCode) {
        this.jdbcTemplate = jdbcTemplate;
        this.payloadService = payloadService;
        this.integrationService = integrationService;
        this.enabled = enabled;
        this.dryRun = dryRun;
        this.endpointCode = endpointCode;
    }

    @Override
    public String name() {
        return "ErpPublish";
    }

    @Override
    public List<String> supports() {
        return List.copyOf(SUPPORTED);
    }

    @Override
    public void onProcessEvent(WfPluginContext context) {
        if (!enabled || !WfPluginContext.ACTION_COMPLETED.equals(context.action())) {
            return;
        }
        String bizType = BIZ_TYPES.get(context.objectType());
        if (bizType == null) {
            return;
        }
        Map<String, Object> object = loadObject(context);
        if (object == null) {
            return;
        }
        Long companyId = longOf(object.get("company_id"));
        String state = str(object.get("lifecycle_state"));
        if (companyId == null || !"Released".equals(state)) {
            log.info("ErpPublish 跳过：对象 {}（{}）生命周期为 {}，只下发已发布对象",
                    object.get("code"), context.objectType(), state);
            return;
        }
        Long endpointId = kingdeeEndpoint(companyId);
        if (endpointId == null) {
            log.info("ErpPublish 跳过：公司 {} 未配置启用的金蝶端点", companyId);
            return;
        }
        try {
            KingdeePayloadService.SourcePayload source =
                    payloadService.build(companyId, bizType, context.objectId(), null);
            IntegrationDtos.JobSubmitResult result = integrationService.submitJob(
                    new IntegrationDtos.JobSubmitReq(endpointId, source.bizType(), source.bizKey(),
                            source.payload(), dryRun, 3),
                    companyId, context.operatorId());
            log.info("ErpPublish 下发：对象 {} → 金蝶任务 {}（bizKey={}，重复={}，dryRun={}）",
                    object.get("code"), result.job().id(), source.bizKey(), result.duplicated(), dryRun);
        } catch (RuntimeException ex) {
            // 插件体异常不影响流程（注册表已兜底），这里补一条业务语义明确的日志
            log.warn("ErpPublish 下发失败：对象 {}（{}）原因：{}",
                    object.get("code"), context.objectType(), ex.getMessage());
        }
    }

    private Map<String, Object> loadObject(WfPluginContext context) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                    SELECT code, name, lifecycle_state, company_id FROM plm_object
                     WHERE id = ? AND object_type = ? AND deleted = false
                    """, context.objectId(), context.objectType());
            return rows.isEmpty() ? null : rows.get(0);
        } catch (RuntimeException ex) {
            log.debug("ErpPublish 查询对象失败：{}", ex.getMessage());
            return null;
        }
    }

    /**
     * 同一公司可能有多个金蝶端点（测试/生产账套）。
     * <p>配置了 {@code endpoint-code} 时按 code 精确选；配了却找不到（停用/删除/改名）就跳过下发，
     * <b>不回退</b>到别的账套——把生产数据推到测试账套或反之，比"这次没下发"严重得多（R4 禁止静默降级）。
     * 未配置时保持既有行为：取 id 最小的启用端点。</p>
     */
    private Long kingdeeEndpoint(Long companyId) {
        if (endpointCode != null && !endpointCode.isBlank()) {
            List<Long> pinned = jdbcTemplate.queryForList("""
                    SELECT id FROM itg_endpoint
                     WHERE company_id = ? AND system_code = 'KINGDEE' AND code = ?
                       AND enabled = true AND deleted = false
                    """, Long.class, companyId, endpointCode.trim());
            if (!pinned.isEmpty()) {
                return pinned.get(0);
            }
            log.warn("ErpPublish 跳过：公司 {} 未找到启用的金蝶端点 code={}，不回退到其它账套",
                    companyId, endpointCode);
            return null;
        }
        List<Long> ids = jdbcTemplate.queryForList("""
                SELECT id FROM itg_endpoint
                 WHERE company_id = ? AND system_code = 'KINGDEE' AND enabled = true AND deleted = false
                 ORDER BY id
                 LIMIT 1
                """, Long.class, companyId);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private static String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static Long longOf(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return value == null ? null : Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
