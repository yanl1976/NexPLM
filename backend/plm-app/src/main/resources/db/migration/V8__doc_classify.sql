-- =====================================================================
-- P2 文档分类（Classify）：分类树 + 分类属性 + 文档归属分类
-- 说明：
--   1. 分类按对象类型（entity_code）区分，同一编码在不同类型下互不影响；
--   2. path 为「/父编码/子编码」枚举，便于按前缀做子树查询与循环校验；
--   3. 分类属性支持必填、枚举、可继承（inheritable=true 时子分类可见/可填）；
--   4. 文档归属分类记在 doc_master.class_id，属性值存 plm_object.ext_json 的 classAttrs。
-- =====================================================================

CREATE TABLE doc_class (
    id          bigint       PRIMARY KEY,
    parent_id   bigint,
    entity_code varchar(64)  NOT NULL DEFAULT 'PdmDoc',
    code        varchar(64)  NOT NULL,
    name        varchar(64)  NOT NULL,
    path        varchar(500) NOT NULL,
    sort        int          NOT NULL DEFAULT 0,
    status      varchar(16)  NOT NULL DEFAULT 'ACTIVE',
    remark      varchar(500),
    version     int          NOT NULL DEFAULT 0,
    deleted     boolean      NOT NULL DEFAULT false,
    created_by  bigint,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_by  bigint,
    updated_at  timestamptz  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_doc_class_code ON doc_class (entity_code, code) WHERE deleted = false;
CREATE INDEX idx_doc_class_parent ON doc_class (parent_id);
CREATE INDEX idx_doc_class_path ON doc_class (entity_code, path);

CREATE TABLE doc_class_attr (
    id            bigint       PRIMARY KEY,
    class_id      bigint       NOT NULL,
    code          varchar(64)  NOT NULL,
    name          varchar(64)  NOT NULL,
    value_type    varchar(32)  NOT NULL,
    enum_code     varchar(64),
    required      boolean      NOT NULL DEFAULT false,
    inheritable   boolean      NOT NULL DEFAULT true,
    default_value varchar(500),
    sort          int          NOT NULL DEFAULT 0,
    deleted       boolean      NOT NULL DEFAULT false,
    created_by    bigint,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    updated_by    bigint,
    updated_at    timestamptz  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_doc_class_attr ON doc_class_attr (class_id, code) WHERE deleted = false;
CREATE INDEX idx_doc_class_attr_class ON doc_class_attr (class_id, sort);

ALTER TABLE doc_master ADD COLUMN class_id bigint;
CREATE INDEX idx_doc_master_class ON doc_master (class_id);

-- 分类值类型枚举
INSERT INTO meta_enum (code, name) VALUES ('DocClassAttrType', '分类属性值类型')
ON CONFLICT DO NOTHING;
INSERT INTO meta_enum_item (enum_code, item_code, item_name, sort) VALUES
    ('DocClassAttrType', 'STRING', '文本', 10),
    ('DocClassAttrType', 'NUMBER', '数值', 20),
    ('DocClassAttrType', 'DATE',   '日期', 30),
    ('DocClassAttrType', 'ENUM',   '枚举', 40),
    ('DocClassAttrType', 'BOOL',   '布尔', 50)
ON CONFLICT DO NOTHING;

-- 示例分类树（贴近机加工/电气的实际文档组织方式，后续可在页面维护）
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort) VALUES
    (910001, NULL,   'PdmDoc', 'MECH',       '机械图纸', '/MECH',                10),
    (910002, 910001, 'PdmDoc', 'MECH-ASSY',  '装配图',   '/MECH/MECH-ASSY',      10),
    (910003, 910001, 'PdmDoc', 'MECH-PART',  '零件图',   '/MECH/MECH-PART',      20),
    (910004, NULL,   'PdmDoc', 'ELEC',       '电气图纸', '/ELEC',                20),
    (910005, NULL,   'PdmDoc', 'PROC',       '工艺文件', '/PROC',                30),
    (910006, NULL,   'PdmDoc', 'STD',        '标准规范', '/STD',                 40)
ON CONFLICT DO NOTHING;

INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, sort) VALUES
    (910101, 910002, 'sheetSize', '图幅',   'ENUM',   'SheetSize', true,  true, 10),
    (910102, 910002, 'material',  '材料',   'STRING', NULL,        false, true, 20),
    (910103, 910003, 'material',  '材料',   'STRING', NULL,        true,  true, 10),
    (910104, 910003, 'surface',   '表面处理', 'STRING', NULL,      false, true, 20),
    (910105, 910004, 'voltage',   '电压等级', 'STRING', NULL,      false, true, 10)
ON CONFLICT DO NOTHING;

INSERT INTO meta_enum (code, name) VALUES ('SheetSize', '图幅')
ON CONFLICT DO NOTHING;
INSERT INTO meta_enum_item (enum_code, item_code, item_name, sort) VALUES
    ('SheetSize', 'A0', 'A0', 10),
    ('SheetSize', 'A1', 'A1', 20),
    ('SheetSize', 'A2', 'A2', 30),
    ('SheetSize', 'A3', 'A3', 40),
    ('SheetSize', 'A4', 'A4', 50)
ON CONFLICT DO NOTHING;
