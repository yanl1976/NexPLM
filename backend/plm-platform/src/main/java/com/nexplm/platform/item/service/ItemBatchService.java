package com.nexplm.platform.item.service;

import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.item.api.ItemDtos;
import com.nexplm.platform.object.domain.PlmObject;
import jakarta.annotation.Nullable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 零部件批量操作：批量新建 / 批量改属性 / 转交所有权。
 *
 * <p>为什么单独成类而不是塞进 {@link ItemService}：批量必须<b>逐条独立事务</b>，
 * 一条失败不能回滚已成功的行；同类内自调用会让 {@code @Transactional} 失效
 * （代理不经过），因此这里作为独立 Bean 逐条调 {@link ItemService}，
 * 每次调用都真正过代理，失败只影响当前行。</p>
 *
 * <p>逐条失败统一转成「部分成功」结果（与 {@code batchDelete} / BOM 批量口径一致），
 * 不向调用方抛 500。</p>
 */
@Service
public class ItemBatchService {

    /** 批量新建时回带上限：避免一次粘贴几百行把提示列表刷爆。 */
    private static final int MAX_MESSAGES = 50;

    private final ItemService itemService;
    private final JdbcTemplate jdbcTemplate;

    public ItemBatchService(ItemService itemService, JdbcTemplate jdbcTemplate) {
        this.itemService = itemService;
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 批量新建：名称/类型缺失的行直接跳过，其余按普通新建逐条落库（各自取号）。 */
    public ItemDtos.BatchResult batchCreate(List<ItemDtos.ItemSaveReq> reqs, Long userId) {
        if (reqs == null || reqs.isEmpty()) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "请至少提供一条零部件记录");
        }
        List<String> messages = new ArrayList<>();
        int done = 0;
        int skipped = 0;
        for (int index = 0; index < reqs.size(); index++) {
            ItemDtos.ItemSaveReq req = reqs.get(index);
            int line = index + 1;
            if (req == null || req.name() == null || req.name().isBlank()) {
                skipped++;
                addMessage(messages, "第 " + line + " 行：名称不能为空");
                continue;
            }
            if (req.itemType() == null || req.itemType().isBlank()) {
                skipped++;
                addMessage(messages, "第 " + line + " 行：类型不能为空");
                continue;
            }
            try {
                ItemDtos.ItemDetail created = itemService.create(req, userId);
                done++;
                addMessage(messages, "第 " + line + " 行：已创建 " + created.code());
            } catch (BizException ex) {
                skipped++;
                addMessage(messages, "第 " + line + " 行：" + ex.getMessage());
            }
        }
        return new ItemDtos.BatchResult(reqs.size(), done, skipped, messages);
    }

    /**
     * 批量改属性：补丁字段 <b>null = 不修改</b>，空串 = 清空。
     * <p>逐条复用普通更新路径，因此「已发布需先修订」「他人检出只读」等规则与单条编辑完全一致。</p>
     */
    public ItemDtos.BatchResult batchUpdate(List<Long> ids, ItemDtos.ItemBatchPatch patch, Long userId) {
        if (ids == null || ids.isEmpty()) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "请至少选择一个零部件");
        }
        if (patch == null) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "请提供要修改的属性");
        }
        List<String> messages = new ArrayList<>();
        int done = 0;
        int skipped = 0;
        for (Long id : ids) {
            String label = String.valueOf(id);
            try {
                ItemDtos.ItemDetail current = itemService.detail(id);
                label = current.code();
                itemService.update(id, merge(current, patch), userId);
                done++;
            } catch (BizException ex) {
                skipped++;
                addMessage(messages, label + "：" + ex.getMessage());
            }
        }
        return new ItemDtos.BatchResult(ids.size(), done, skipped, messages);
    }

    /**
     * 转交所有权：把选中零部件的属主改为目标用户。
     * <p>发布状态与版本都不动（属主不是业务内容），但跨公司不允许——
     * 属主变更会让对象出现在对方的数据作用域里，跨公司等于越权共享。</p>
     */
    public ItemDtos.BatchResult transferOwner(List<Long> ids, Long targetUserId, Long operatorId) {
        if (ids == null || ids.isEmpty()) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "请至少选择一个零部件");
        }
        if (targetUserId == null) {
            throw BizException.of(ErrorCode.COMMON_BAD_REQUEST, "请选择接手人");
        }
        List<Object[]> found = jdbcTemplate.query("""
                        SELECT display_name, company_id FROM sys_user WHERE id = ?
                        """,
                (rs, rowNum) -> new Object[]{
                        rs.getString("display_name") == null ? String.valueOf(targetUserId) : rs.getString("display_name"),
                        rs.getObject("company_id", Long.class)},
                targetUserId);
        if (found.isEmpty()) {
            throw BizException.of(ErrorCode.COMMON_NOT_FOUND, "接手人不存在：" + targetUserId);
        }
        Long targetCompanyId = (Long) found.get(0)[1];
        String targetName = String.valueOf(found.get(0)[0]);

        List<String> messages = new ArrayList<>();
        int done = 0;
        int skipped = 0;
        for (Long id : ids) {
            String label = String.valueOf(id);
            try {
                PlmObject object = itemService.requireItem(id);
                label = object.getCode();
                if (targetUserId.equals(object.getOwnerId())) {
                    skipped++;
                    addMessage(messages, label + "：已经是 " + targetName + " 所有");
                    continue;
                }
                if (targetCompanyId != null && object.getCompanyId() != null
                        && !targetCompanyId.equals(object.getCompanyId())) {
                    skipped++;
                    addMessage(messages, label + "：接手人与对象不属于同一公司，不能转交");
                    continue;
                }
                jdbcTemplate.update("""
                        UPDATE plm_object SET owner_id = ?, updated_by = ?, updated_at = now() WHERE id = ?
                        """, targetUserId, operatorId, id);
                done++;
            } catch (BizException ex) {
                skipped++;
                addMessage(messages, label + "：" + ex.getMessage());
            }
        }
        return new ItemDtos.BatchResult(ids.size(), done, skipped, messages);
    }

    // ------------------------------------------------------------ 内部

    /** 补丁合并：null 保留原值，非 null（含空串）覆盖。 */
    private ItemDtos.ItemSaveReq merge(ItemDtos.ItemDetail current, ItemDtos.ItemBatchPatch patch) {
        return new ItemDtos.ItemSaveReq(
                pick(patch.code(), current.code()),
                pick(patch.name(), current.name()),
                pick(patch.itemType(), current.itemType()),
                pick(patch.specification(), current.specification()),
                pick(patch.material(), current.material()),
                pick(patch.unit(), current.unit()),
                patch.weight() != null ? patch.weight() : current.weight(),
                pick(patch.sourceType(), current.sourceType()),
                pick(patch.stdCode(), current.stdCode()),
                pick(patch.remark(), current.remark()),
                current.orgId(),
                patch.securityLevel() != null ? patch.securityLevel() : current.securityLevel(),
                mergeDesignAttr(current.designAttr(), patch.designAttr()));
    }

    /** 设计属性补丁：整块为 null 视为不改；块内字段 null 同样保留原值。 */
    private ItemDtos.ItemDesignAttr mergeDesignAttr(@Nullable ItemDtos.ItemDesignAttr origin,
                                                    @Nullable ItemDtos.ItemDesignAttr patch) {
        if (patch == null) {
            return origin;
        }
        if (origin == null) {
            return patch;
        }
        return new ItemDtos.ItemDesignAttr(
                pick(patch.provider(), origin.provider()),
                pick(patch.dwgNo(), origin.dwgNo()),
                pick(patch.productCategory(), origin.productCategory()),
                pick(patch.productModel(), origin.productModel()),
                pick(patch.productSpec(), origin.productSpec()),
                pick(patch.materialType(), origin.materialType()),
                pick(patch.materialType2(), origin.materialType2()),
                pick(patch.surfaceTreatment(), origin.surfaceTreatment()),
                patch.isTooling() != null ? patch.isTooling() : Boolean.TRUE.equals(origin.isTooling()),
                pick(patch.weightUnit(), origin.weightUnit()),
                pick(patch.montage(), origin.montage()),
                patch.colorItem() != null ? patch.colorItem() : Boolean.TRUE.equals(origin.colorItem()));
    }

    private String pick(String patchValue, String originValue) {
        return patchValue != null ? patchValue : originValue;
    }

    private void addMessage(List<String> messages, String message) {
        if (messages.size() < MAX_MESSAGES) {
            messages.add(message);
        }
    }
}
