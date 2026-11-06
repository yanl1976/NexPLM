-- =====================================================================
-- P3+ 零部件页面复刻（对齐旧系统 PSEITEM / 见 docs/10-深度探查-零部件管理.md）
-- 说明：
--   1. item_master 增 using_state（旧系统 STATEID 使用状态：Using/StopUsing/GiveUp）；
--   2. 生命周期阶段 phase_state 复用 plm_object 现有列，取值补齐 S/A/B（试制/小批/量产）；
--   3. 设计属性独立表 item_design_attr（旧系统 PSEBASEFORM 子表单），避免主表随表单类型膨胀；
--   4. 零件视图层 item_view（旧系统 PSEITEMVIEW）：一个零件可有多个视图，BOM 仍按 view_type 归属；
--   5. meta_tab 重排为 10 个页签，与旧系统对象查看器一致（属性/视图/设计文档/可视化/BOM/被引用/历史/流程/权限/日志）。
-- =====================================================================

ALTER TABLE item_master ADD COLUMN IF NOT EXISTS using_state varchar(20) NOT NULL DEFAULT 'Using';
CREATE INDEX IF NOT EXISTS idx_item_master_using ON item_master (using_state);

-- ---------------------------------------------------------------------
-- 设计属性：与 item_master 1:1，字段对齐旧系统 PSEBASEFORM（不重复主表已有的规格/材料/标准号/备注）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS item_design_attr (
    object_id         bigint       PRIMARY KEY,
    provider          varchar(100),
    dwg_no            varchar(100),
    product_category  varchar(100),
    product_model     varchar(100),
    product_spec      varchar(100),
    material_type     varchar(100),
    material_type2    varchar(100),
    surface_treatment varchar(100),
    is_tooling        boolean      NOT NULL DEFAULT false,
    weight_unit       varchar(20),
    montage           varchar(100),
    color_item        boolean      NOT NULL DEFAULT false,
    created_at        timestamptz  NOT NULL DEFAULT now(),
    updated_at        timestamptz  NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------
-- 零件视图：旧系统「零件 → 视图 → BOM/文档」三层中的中间层
-- 只承载视图级属性；BOM 行仍以 (parent_object_id, view_type) 归属，避免破坏既有结构
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS item_view (
    id             bigint       PRIMARY KEY,
    item_object_id bigint       NOT NULL,
    view_type      varchar(32)  NOT NULL DEFAULT 'DESIGN',
    description    varchar(500),
    is_default     boolean      NOT NULL DEFAULT false,
    source_view_id bigint,
    deleted        boolean      NOT NULL DEFAULT false,
    created_by     bigint,
    created_at     timestamptz  NOT NULL DEFAULT now(),
    updated_by     bigint,
    updated_at     timestamptz  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_item_view ON item_view (item_object_id, view_type) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_item_view_item ON item_view (item_object_id, is_default DESC, view_type);

-- ---------------------------------------------------------------------
-- 枚举：使用状态 / 生命周期阶段 / 视图类型
-- ---------------------------------------------------------------------
INSERT INTO meta_enum (code, name) VALUES
    ('ItemUsingState', '零部件使用状态'),
    ('ItemPhaseState', '零部件生命周期阶段'),
    ('ItemViewType',   '零部件视图类型')
ON CONFLICT DO NOTHING;

INSERT INTO meta_enum_item (enum_code, item_code, item_name, sort) VALUES
    ('ItemUsingState', 'Using',     '在用',   10),
    ('ItemUsingState', 'StopUsing', '停用',   20),
    ('ItemUsingState', 'GiveUp',    '废弃',   30),
    ('ItemPhaseState', 'S', '试制', 10),
    ('ItemPhaseState', 'A', '小批', 20),
    ('ItemPhaseState', 'B', '量产', 30),
    ('ItemViewType',   'DESIGN', '设计视图', 10),
    ('ItemViewType',   'PROCESS', '工艺视图', 20),
    ('ItemViewType',   'MANUFACTURE', '制造视图', 30)
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- 元数据：PdmItem 能力 + 设计属性字段 + 10 个页签
-- ---------------------------------------------------------------------
UPDATE meta_entity
   SET capabilities = '["createPermission","advanceQuery","revision","checkout","versions","acl","logs","views"]'::jsonb
 WHERE code = 'PdmItem';

INSERT INTO meta_field (id, entity_code, code, name, value_type, data_type, required, enum_code,
                        ui_json, searchable, sort, version_no, status)
VALUES
    (903001, 'PdmItem', 'provider',          '供应商',     'DefaultObjectValue', 'VARCHAR', false, NULL, '{}'::jsonb, true,  90,  1, 'PUBLISHED'),
    (903002, 'PdmItem', 'dwgNo',             '图号',       'DefaultObjectValue', 'VARCHAR', false, NULL, '{}'::jsonb, true,  100, 1, 'PUBLISHED'),
    (903003, 'PdmItem', 'productCategory',   '产品类别',   'DefaultObjectValue', 'VARCHAR', false, NULL, '{}'::jsonb, false, 110, 1, 'PUBLISHED'),
    (903004, 'PdmItem', 'productModel',      '产品型号',   'DefaultObjectValue', 'VARCHAR', false, NULL, '{}'::jsonb, false, 120, 1, 'PUBLISHED'),
    (903005, 'PdmItem', 'productSpec',       '产品规格',   'DefaultObjectValue', 'VARCHAR', false, NULL, '{}'::jsonb, false, 130, 1, 'PUBLISHED'),
    (903006, 'PdmItem', 'materialType',      '毛坯种类',   'DefaultObjectValue', 'VARCHAR', false, NULL, '{}'::jsonb, true,  140, 1, 'PUBLISHED'),
    (903007, 'PdmItem', 'materialType2',     '毛坯种类2',  'DefaultObjectValue', 'VARCHAR', false, NULL, '{}'::jsonb, false, 150, 1, 'PUBLISHED'),
    (903008, 'PdmItem', 'surfaceTreatment',  '表面处理',   'DefaultObjectValue', 'VARCHAR', false, NULL, '{}'::jsonb, true,  160, 1, 'PUBLISHED'),
    (903009, 'PdmItem', 'isTooling',         '是否工装',   'BoolValue',          'BOOLEAN', false, NULL, '{}'::jsonb, false, 170, 1, 'PUBLISHED'),
    (903010, 'PdmItem', 'weightUnit',        '重量单位',   'DefaultObjectValue', 'VARCHAR', false, NULL, '{}'::jsonb, false, 180, 1, 'PUBLISHED'),
    (903011, 'PdmItem', 'montage',           '所属装配',   'DefaultObjectValue', 'VARCHAR', false, NULL, '{}'::jsonb, false, 190, 1, 'PUBLISHED'),
    (903012, 'PdmItem', 'colorItem',         '是否颜色件', 'BoolValue',          'BOOLEAN', false, NULL, '{}'::jsonb, false, 200, 1, 'PUBLISHED')
ON CONFLICT (entity_code, code) DO NOTHING;

DELETE FROM meta_tab WHERE entity_code = 'PdmItem';
INSERT INTO meta_tab (id, entity_code, code, name, tab_type, permission_code, sort) VALUES
    (902101, 'PdmItem', 'props',     '属性',     'PROPS',     'pse.item.read', 10),
    (902102, 'PdmItem', 'views',     '视图',     'VIEWS',     'pse.item.read', 20),
    (902103, 'PdmItem', 'docs',      '设计文档', 'DOCS',      'pse.item.read', 30),
    (902104, 'PdmItem', 'visual',    '可视化',   'VISUAL',    'pse.item.read', 40),
    (902105, 'PdmItem', 'bom',       'BOM',      'BOM',       'pse.item.read', 50),
    (902106, 'PdmItem', 'whereused', '被引用',   'WHEREUSED', 'pse.item.read', 60),
    (902107, 'PdmItem', 'versions',  '历史',     'VERSIONS',  'pse.item.read', 70),
    (902108, 'PdmItem', 'workflow',  '流程',     'WORKFLOW',  'pse.item.read', 80),
    (902109, 'PdmItem', 'acl',       '权限',     'ACL',       'pse.item.read', 90),
    (902110, 'PdmItem', 'logs',      '日志',     'LOGS',      'pse.item.read', 100);

-- 存量：老零件补建默认设计视图（保证「视图」页签不空）
INSERT INTO item_view (id, item_object_id, view_type, description, is_default, created_at, updated_at)
SELECT (900000000000000000 + row_number() OVER (ORDER BY o.id))::bigint,
       o.id, 'DESIGN', '默认设计视图', true, now(), now()
  FROM plm_object o
 WHERE o.object_type = 'PdmItem' AND o.is_latest = true AND o.deleted = false
   AND NOT EXISTS (SELECT 1 FROM item_view v WHERE v.item_object_id = o.id AND v.deleted = false);
