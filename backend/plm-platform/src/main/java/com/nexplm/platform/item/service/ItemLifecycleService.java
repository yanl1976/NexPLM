package com.nexplm.platform.item.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.nexplm.common.exception.BizException;
import com.nexplm.common.exception.ErrorCode;
import com.nexplm.platform.doc.service.CheckoutService;
import com.nexplm.platform.item.api.ItemDtos;
import com.nexplm.platform.item.domain.ItemMaster;
import com.nexplm.platform.item.mapper.ItemMasterMapper;
import com.nexplm.platform.object.domain.PlmObject;
import com.nexplm.platform.object.mapper.PlmObjectMapper;
import com.nexplm.platform.object.version.VersionFormatter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 零部件的检出/检入/修订与版本历史（旧系统 PSEViewsMenu 的 CheckOut / CheckIn / UnCheckOut / Emend）。
 * <p>检出锁复用通用 {@link CheckoutService}（plm_checkout 按 object_id），与图文档同一套规则。</p>
 */
@Service
public class ItemLifecycleService {

    /** 已发布/已废弃不可检出编辑，必须先修订。 */
    private static final Set<String> LOCKED_STATES = Set.of("Released", "Obsolete");

    private final PlmObjectMapper objectMapper;
    private final ItemMasterMapper itemMapper;
    private final CheckoutService checkoutService;
    private final ItemService itemService;
    private final JdbcTemplate jdbcTemplate;

    public ItemLifecycleService(PlmObjectMapper objectMapper, ItemMasterMapper itemMapper,
                                CheckoutService checkoutService, ItemService itemService,
                                JdbcTemplate jdbcTemplate) {
        this.objectMapper = objectMapper;
        this.itemMapper = itemMapper;
        this.checkoutService = checkoutService;
        this.itemService = itemService;
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 检出：拿到编辑锁。重复检出视为续期；他人持有冲突。 */
    @Transactional
    public ItemDtos.ItemDetail checkout(Long id, Long userId, String clientIp, String machine) {
        PlmObject object = itemService.requireItem(id);
        if (LOCKED_STATES.contains(object.getLifecycleState())) {
            throw BizException.of(ErrorCode.OBJ_STATE_TRANSITION_DENIED,
                    "零部件处于「" + object.getLifecycleState() + "」状态，请先修订再检出");
        }
        checkoutService.acquire(id, userId, clientIp, machine, null);
        return itemService.detail(id);
    }

    /**
     * 检入：产生一个新小版本（iteration+1），旧版本保留可追溯。
     * 与图文档口径一致：检入不升大版本，大版本只在「修订」时递增。
     */
    @Transactional
    public ItemDtos.ItemDetail checkin(Long id, Long userId) {
        PlmObject current = itemService.requireItem(id);
        CheckoutService.Lock lock = checkoutService.active(id)
                .orElseThrow(() -> BizException.of(ErrorCode.COMMON_CONFLICT, "零部件未被检出，无法检入"));
        if (!lock.userId().equals(userId)) {
            throw BizException.of(ErrorCode.COMMON_FORBIDDEN,
                    "该零部件由他人检出（用户 " + lock.userId() + "），不能代为检入");
        }
        long newId = copyVersion(current, current.getRevision(),
                current.getIteration() == null ? 2 : current.getIteration() + 1,
                current.getLifecycleState(), userId);
        checkoutService.release(id, userId);
        return itemService.detail(newId);
    }

    /** 撤销检出：放弃本次编辑，不产生新版本。 */
    @Transactional
    public void cancelCheckout(Long id, Long userId) {
        itemService.requireItem(id);
        checkoutService.release(id, userId);
    }

    /**
     * 修订：大版本 +1，回到草稿态（旧系统 Emend 语义）。
     * 只有「已发布」可以直接修订；草稿态无需修订，直接编辑即可。
     */
    @Transactional
    public ItemDtos.ItemDetail revise(Long id, Long userId) {
        PlmObject current = itemService.requireItem(id);
        if (!"Released".equals(current.getLifecycleState())) {
            throw BizException.of(ErrorCode.OBJ_STATE_TRANSITION_DENIED,
                    "仅「已发布」的零部件可以修订，当前状态：" + current.getLifecycleState());
        }
        long newId = copyVersion(current, current.getRevision() + 1, 1, "Draft", userId);
        if (checkoutService.active(id).isPresent()) {
            checkoutService.forceRelease(id);
        }
        return itemService.detail(newId);
    }

    /** 版本历史：同一 masterId 下的所有版本，按大版本、小版本倒序。 */
    public List<ItemDtos.VersionRow> versions(Long id) {
        PlmObject current = itemService.requireItem(id);
        Long masterId = current.getMasterId() == null ? current.getId() : current.getMasterId();
        List<PlmObject> rows = objectMapper.selectList(new LambdaQueryWrapper<PlmObject>()
                .eq(PlmObject::getObjectType, ItemService.OBJECT_TYPE)
                .eq(PlmObject::getMasterId, masterId)
                .orderByDesc(PlmObject::getRevision)
                .orderByDesc(PlmObject::getIteration));
        return rows.stream()
                .map(object -> new ItemDtos.VersionRow(object.getId(), object.getRevision(),
                        object.getIteration(),
                        VersionFormatter.label(object.getRevision(), object.getIteration()),
                        object.getLifecycleState(), object.getIsLatest(), object.getCreatedBy(),
                        createdByName(object.getCreatedBy()), object.getCreatedAt(), object.getReleasedAt()))
                .toList();
    }

    // ------------------------------------------------------------ 内部

    /**
     * 以当前版本为模板生成新版本行，并复制子表（主数据 / 设计属性 / 视图）。
     * 旧行 is_latest 置 false，新行 masters 指向同一 masterId。
     */
    private long copyVersion(PlmObject source, int revision, int iteration, String lifecycleState, Long userId) {
        OffsetDateTime now = OffsetDateTime.now();
        long newId = IdWorker.getId();

        PlmObject created = new PlmObject();
        created.setId(newId);
        created.setObjectType(ItemService.OBJECT_TYPE);
        created.setMasterId(source.getMasterId() == null ? source.getId() : source.getMasterId());
        created.setCode(source.getCode());
        created.setName(source.getName());
        created.setCompanyId(source.getCompanyId());
        created.setOrgId(source.getOrgId());
        created.setOwnerId(source.getOwnerId() == null ? userId : source.getOwnerId());
        created.setOwnerGroupId(source.getOwnerGroupId());
        created.setSecurityLevel(source.getSecurityLevel());
        created.setLifecycleState(lifecycleState);
        created.setPhaseState(source.getPhaseState());
        created.setWorkflowState("Released".equals(lifecycleState) ? source.getWorkflowState() : "New");
        created.setRevision(revision);
        created.setIteration(iteration);
        created.setIsLatest(true);
        created.setReleasedAt(source.getReleasedAt());
        created.setExtJson(source.getExtJson());
        created.setVersion(0);
        created.setDeleted(false);
        created.setCreatedBy(userId);
        created.setCreatedAt(now);
        created.setUpdatedBy(userId);
        created.setUpdatedAt(now);
        objectMapper.insert(created);

        // 旧版本让位：同一 master 只能有一个 is_latest
        jdbcTemplate.update("""
                UPDATE plm_object SET is_latest = false, updated_at = now()
                 WHERE object_type = ? AND master_id = ? AND id <> ?
                """, ItemService.OBJECT_TYPE, created.getMasterId(), newId);

        ItemMaster master = itemMapper.selectById(source.getId());
        if (master != null) {
            ItemMaster copy = new ItemMaster();
            copy.setObjectId(newId);
            copy.setItemType(master.getItemType());
            copy.setSpecification(master.getSpecification());
            copy.setMaterial(master.getMaterial());
            copy.setUnit(master.getUnit());
            copy.setWeight(master.getWeight());
            copy.setSourceType(master.getSourceType());
            copy.setStdCode(master.getStdCode());
            copy.setRemark(master.getRemark());
            copy.setUsingState(master.getUsingState());
            copy.setCreatedAt(now);
            copy.setUpdatedAt(now);
            itemMapper.insert(copy);
        }

        jdbcTemplate.update("""
                        INSERT INTO item_design_attr (object_id, provider, dwg_no, product_category,
                                                      product_model, product_spec, material_type, material_type2,
                                                      surface_treatment, is_tooling, weight_unit, montage,
                                                      color_item, created_at, updated_at)
                        SELECT ?, provider, dwg_no, product_category, product_model, product_spec,
                               material_type, material_type2, surface_treatment, is_tooling,
                               weight_unit, montage, color_item, ?, ?
                          FROM item_design_attr WHERE object_id = ?
                        """, newId, now, now, source.getId());

        // 视图层随版本复制：保持「新版本同样有 Design 等视图」
        jdbcTemplate.update("""
                        INSERT INTO item_view (id, item_object_id, view_type, description, is_default,
                                               source_view_id, created_by, created_at, updated_by, updated_at)
                        SELECT ? + row_number() OVER (ORDER BY is_default DESC, view_type),
                               ?, view_type, description, is_default, source_view_id, ?, ?, ?, ?
                          FROM item_view WHERE item_object_id = ? AND deleted = false
                        """, IdWorker.getId(), newId, userId, now, userId, now, source.getId());

        return newId;
    }

    private String createdByName(Long userId) {
        if (userId == null) {
            return null;
        }
        return jdbcTemplate.query("SELECT display_name FROM sys_user WHERE id = ?",
                rs -> rs.next() ? rs.getString(1) : null, userId);
    }

    /** 供前端字典展示：把视图类型统一成大写码。 */
    String normalizeViewType(String viewType) {
        return viewType == null || viewType.isBlank() ? "DESIGN" : viewType.trim().toUpperCase(Locale.ROOT);
    }
}
