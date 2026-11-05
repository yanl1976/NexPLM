-- =====================================================================
-- P3 零部件：item_master 子表 + PdmItem 对象类型 + 枚举 + 编码序列
-- 说明：
--   1. 零部件与图文档一样复用 plm_object 主表，item_master 为 1:1 结构子表；
--   2. 8 种零部件类型复用 V2 的 ItemType 枚举（产品/部件/零件/标准件/工装/量具/模具/外购件）；
--   3. 供应方式 MAKE（自制）/ BUY（外购）单独成枚举，便于按类型筛选与后续 ERP 联动；
--   4. 编码先用简码 PN-000001，完整编码规则引擎在后续接入。
-- =====================================================================

CREATE TABLE item_master (
    object_id     bigint       PRIMARY KEY,
    item_type     varchar(32)  NOT NULL,
    specification varchar(500),
    material      varchar(64),
    unit          varchar(32)  NOT NULL DEFAULT 'EA',
    weight        numeric(18,4),
    source_type   varchar(32)  NOT NULL DEFAULT 'MAKE',
    std_code      varchar(128),
    remark        varchar(500),
    created_at    timestamptz  NOT NULL DEFAULT now(),
    updated_at    timestamptz  NOT NULL DEFAULT now()
);
CREATE INDEX idx_item_master_type ON item_master (item_type, source_type);

INSERT INTO meta_enum (code, name) VALUES ('ItemSourceType', '零部件供应方式')
ON CONFLICT DO NOTHING;
INSERT INTO meta_enum_item (enum_code, item_code, item_name, sort) VALUES
    ('ItemSourceType', 'MAKE', '自制', 10),
    ('ItemSourceType', 'BUY',  '外购', 20)
ON CONFLICT DO NOTHING;

INSERT INTO meta_entity (id, code, name, name_en, table_name, abstract_flag, capabilities,
                         version_policy, iteration_policy, lifecycle_code, workflow_allowed,
                         security_level, schema_json, version_no, status, remark)
VALUES (9002, 'PdmItem', '零部件', 'Item', 'plm_object', false,
        '["createPermission","advanceQuery","revision","checkout"]'::jsonb,
        'LETTER', 'NUMBER', 'DEFAULT_LIFECYCLE', true, 0, '{}'::jsonb, 1, 'PUBLISHED',
        '产品/部件/零件/标准件/工装/量具/模具/外购件共用，类型由 itemType 区分')
ON CONFLICT (code) DO NOTHING;

INSERT INTO meta_field (id, entity_code, code, name, value_type, data_type, required, enum_code,
                        ui_json, searchable, sort, version_no, status)
VALUES
    (90021, 'PdmItem', 'itemType',      '零部件类型', 'EnumValue',          'VARCHAR', true,  'ItemType',
     '{}'::jsonb, true,  10, 1, 'PUBLISHED'),
    (90022, 'PdmItem', 'specification', '规格型号',   'DefaultObjectValue', 'VARCHAR', false, NULL,
     '{}'::jsonb, true,  20, 1, 'PUBLISHED'),
    (90023, 'PdmItem', 'material',      '材料',       'DefaultObjectValue', 'VARCHAR', false, NULL,
     '{}'::jsonb, false, 30, 1, 'PUBLISHED'),
    (90024, 'PdmItem', 'unit',          '计量单位',   'DefaultObjectValue', 'VARCHAR', true,  NULL,
     '{}'::jsonb, false, 40, 1, 'PUBLISHED'),
    (90025, 'PdmItem', 'weight',        '单件重量',   'DefaultObjectValue', 'NUMERIC', false, NULL,
     '{}'::jsonb, false, 50, 1, 'PUBLISHED'),
    (90026, 'PdmItem', 'sourceType',    '供应方式',   'EnumValue',          'VARCHAR', true,  'ItemSourceType',
     '{}'::jsonb, true,  60, 1, 'PUBLISHED'),
    (90027, 'PdmItem', 'stdCode',       '标准号',     'DefaultObjectValue', 'VARCHAR', false, NULL,
     '{}'::jsonb, true,  70, 1, 'PUBLISHED'),
    (90028, 'PdmItem', 'remark',        '备注',       'DescriptionValue',   'TEXT',    false, NULL,
     '{}'::jsonb, false, 80, 1, 'PUBLISHED')
ON CONFLICT (entity_code, code) DO NOTHING;

INSERT INTO meta_tab (id, entity_code, code, name, tab_type, permission_code, sort) VALUES
    (902001, 'PdmItem', 'props',    '属性',   'PROPS',    'pse.item.read', 10),
    (902002, 'PdmItem', 'bom',      'BOM',    'BOM',      'pse.item.read', 20),
    (902003, 'PdmItem', 'whereused','被引用', 'WHEREUSED','pse.item.read', 30),
    (902004, 'PdmItem', 'docs',     '相关文档','DOCS',    'pse.item.read', 40),
    (902005, 'PdmItem', 'logs',     '日志',   'LOGS',     'pse.item.read', 50)
ON CONFLICT (entity_code, code) DO NOTHING;

INSERT INTO code_sequence (id, rule_code, scope_key, current_no) VALUES (900201, 'PdmItem', 'DEFAULT', 0)
ON CONFLICT (rule_code, scope_key) DO NOTHING;

-- ---------------------------------------------------------------------
-- BOM 行：同一父件 + 视图 + 子件唯一（重复子件由唯一索引兜底，服务层给出友好提示）；
-- 设计/工艺/制造三视图共用一张表，靠 view_type 区分。
-- ---------------------------------------------------------------------
CREATE TABLE bom_line (
    id               bigint        PRIMARY KEY,
    parent_object_id bigint        NOT NULL,
    child_object_id  bigint        NOT NULL,
    view_type        varchar(16)   NOT NULL DEFAULT 'DESIGN',
    line_no          int           NOT NULL DEFAULT 10,
    qty              numeric(18,6) NOT NULL DEFAULT 1,
    unit             varchar(32),
    loss_rate        numeric(6,3)  NOT NULL DEFAULT 0,
    ref_designator   varchar(500),
    remark           varchar(500),
    version          int           NOT NULL DEFAULT 0,
    deleted          boolean       NOT NULL DEFAULT false,
    created_by       bigint,
    created_at       timestamptz   NOT NULL DEFAULT now(),
    updated_by       bigint,
    updated_at       timestamptz   NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_bom_line ON bom_line (parent_object_id, view_type, child_object_id)
    WHERE deleted = false;
CREATE INDEX idx_bom_line_parent ON bom_line (parent_object_id, view_type, line_no);
CREATE INDEX idx_bom_line_child ON bom_line (child_object_id);
