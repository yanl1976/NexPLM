-- =====================================================================
-- 批次 H·T-H-01 零部件分类（方案 A：复用 doc_class 分类表，按 entity_code 隔离）
-- 决策见 docs/adr/0001-零部件分类复用文档分类表.md
-- 取证见 docs/06 EXP-20261108-01（蓝本 Classify/ClassifyUse）
--
-- 说明：
--   1. 分类树与分类属性继续用 doc_class / doc_class_attr，靠 entity_code='PdmItem' 与文档分类隔离；
--      两表由 DocClassService 独占写入（单一口径），不在零部件模块复制一套。
--   2. 零件归属分类记在 item_master.class_id（与 doc_master.class_id 对称）。
--   3. 分类属性值存 plm_object.ext_json 的 classAttrs（与文档一致，写法复用 DocService）。
--   4. 属性值类型枚举复用 DocClassAttrType（文本/数值/日期/枚举/布尔）。
-- =====================================================================

ALTER TABLE item_master ADD COLUMN IF NOT EXISTS class_id bigint;
CREATE INDEX IF NOT EXISTS idx_item_master_class ON item_master (class_id);
COMMENT ON COLUMN item_master.class_id IS '零部件分类（doc_class.id，entity_code=PdmItem）';

-- 分类维护权限；查看沿用 pse.item.read（对应蓝本 Classify 维护 / ClassifyUse 应用两档中的维护档）
INSERT INTO sys_permission (code, module, action, name, name_en, perm_type) VALUES
    ('pse.classify.manage', 'PSE', 'CLASSIFY', '零部件分类维护', 'Item classify manage', 'FUNCTION')
ON CONFLICT DO NOTHING;

-- 系统管理员(10) 与业务用户(13) 默认授予（蓝本的部门授权 PERGROUP 首版不做，见 GAP(T-H-01)）
INSERT INTO sys_role_permission (role_id, permission_code) VALUES
    (10, 'pse.classify.manage'),
    (13, 'pse.classify.manage')
ON CONFLICT DO NOTHING;

-- 示例分类树（与文档分类同一套表，entity_code=PdmItem；名称带“示例”，页面可改名/删除）
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, remark) VALUES
    (920001, NULL,   'PdmItem', 'MECH',      '机械件（示例）', '/MECH',           10, '示例分类，可在页面改名或删除'),
    (920002, 920001, 'PdmItem', 'MECH-STD',  '标准件',         '/MECH/MECH-STD',  10, '示例分类，可在页面改名或删除'),
    (920003, 920001, 'PdmItem', 'MECH-CUST', '自制件',         '/MECH/MECH-CUST', 20, '示例分类，可在页面改名或删除'),
    (920004, NULL,   'PdmItem', 'ELEC',      '电气件（示例）', '/ELEC',           20, '示例分类，可在页面改名或删除'),
    (920005, 920004, 'PdmItem', 'ELEC-CONN', '连接器',         '/ELEC/ELEC-CONN', 10, '示例分类，可在页面改名或删除'),
    (920006, NULL,   'PdmItem', 'RAW',       '原材料（示例）', '/RAW',            30, '示例分类，可在页面改名或删除')
ON CONFLICT DO NOTHING;

INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, sort) VALUES
    (920101, 920002, 'material', '材料',   'STRING', NULL, false, true, 10),
    (920102, 920002, 'standard', '标准号', 'STRING', NULL, false, true, 20),
    (920103, 920003, 'material', '材料',   'STRING', NULL, true,  true, 10),
    (920104, 920003, 'surface',  '表面处理','STRING', NULL, false, true, 20),
    (920105, 920005, 'pitch',    '间距',   'NUMBER', NULL, false, true, 10)
ON CONFLICT DO NOTHING;
