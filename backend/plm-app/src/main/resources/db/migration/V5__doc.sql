-- =====================================================================
-- P2 图文档：doc_master 子表 + PdmDoc 对象类型 + 文档相关枚举
-- 说明：doc_master 与 plm_object 为 1:1（object_id 即主键）；
--       业务字段（文档类型/来源/关键词/描述）存 plm_object.ext_json，
--       子表只放结构性统计（文件数、主文件、页数），便于列表与检索加速。
-- =====================================================================

CREATE TABLE doc_master (
    object_id    bigint      PRIMARY KEY,
    doc_type     varchar(32) NOT NULL DEFAULT 'DESIGN',
    vault_id     bigint,
    file_ext     varchar(32),
    file_count   int         NOT NULL DEFAULT 0,
    page_count   int,
    source       varchar(16) NOT NULL DEFAULT 'NORMAL',
    is_derived   boolean     NOT NULL DEFAULT false,
    main_file_id bigint,
    updated_at   timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_doc_master_type ON doc_master (doc_type, source);

-- 文档枚举
INSERT INTO meta_enum (code, name) VALUES
    ('DocType',   '文档类型'),
    ('DocSource', '文档来源')
ON CONFLICT DO NOTHING;

INSERT INTO meta_enum_item (enum_code, item_code, item_name, sort) VALUES
    ('DocType', 'DESIGN',   '设计图纸', 10),
    ('DocType', 'TECH',     '技术文件', 20),
    ('DocType', 'PROCESS',  '工艺文件', 30),
    ('DocType', 'STANDARD', '标准规范', 40),
    ('DocType', 'OTHER',    '其他',     90),
    ('DocSource', 'NORMAL', '普通文档', 10),
    ('DocSource', 'CAD',    'CAD 文档', 20)
ON CONFLICT DO NOTHING;

-- 图文档对象类型（普通文档与 CAD 文档共用，靠 docType/source 区分子类型）
INSERT INTO meta_entity (id, code, name, name_en, table_name, abstract_flag, capabilities,
                         version_policy, iteration_policy, lifecycle_code, workflow_allowed,
                         security_level, schema_json, version_no, status, remark)
VALUES (9001, 'PdmDoc', '图文档', 'Document', 'plm_object', false,
        '["createPermission","advanceQuery","revision","checkout"]'::jsonb,
        'LETTER', 'NUMBER', 'DEFAULT_LIFECYCLE', true, 0, '{}'::jsonb, 1, 'PUBLISHED',
        '普通文档与 CAD 文档共用的对象类型，子类型由 doc_type/source 区分')
ON CONFLICT (code) DO NOTHING;

INSERT INTO meta_field (id, entity_code, code, name, value_type, data_type, required, enum_code,
                        ui_json, searchable, sort, version_no, status)
VALUES
    (90011, 'PdmDoc', 'docType',     '文档类型', 'EnumValue',          'VARCHAR', true,  'DocType',
     '{}'::jsonb, true,  10, 1, 'PUBLISHED'),
    (90012, 'PdmDoc', 'source',      '文档来源', 'EnumValue',          'VARCHAR', true,  'DocSource',
     '{}'::jsonb, false, 20, 1, 'PUBLISHED'),
    (90013, 'PdmDoc', 'keyword',     '关键词',   'DefaultObjectValue', 'VARCHAR', false, NULL,
     '{}'::jsonb, true,  30, 1, 'PUBLISHED'),
    (90014, 'PdmDoc', 'description', '描述',     'DescriptionValue',   'TEXT',    false, NULL,
     '{}'::jsonb, false, 40, 1, 'PUBLISHED')
ON CONFLICT (entity_code, code) DO NOTHING;

-- 文档编码序列（P2 先用简码 DOC-000001，完整编码规则引擎在后续阶段接入）
INSERT INTO code_sequence (id, rule_code, scope_key, current_no) VALUES (900101, 'PdmDoc', 'DEFAULT', 0)
ON CONFLICT (rule_code, scope_key) DO NOTHING;
