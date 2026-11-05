-- =====================================================================
-- P2 对象查看器：元数据驱动的页签定义（meta_tab）
-- 说明：页签是「对象类型」级配置，前端按返回顺序渲染；
--       tab_type 决定前端用哪个组件渲染（PROPS/FILES/VERSIONS/RELATIONS/WORKFLOW/ACL/LOGS/CUSTOM）。
--       未配置页签的对象类型由后端回退到内置默认页签，保证老数据可用。
-- =====================================================================

CREATE TABLE meta_tab (
    id              bigint       PRIMARY KEY,
    entity_code     varchar(64)  NOT NULL,
    code            varchar(64)  NOT NULL,
    name            varchar(64)  NOT NULL,
    tab_type        varchar(32)  NOT NULL,
    permission_code varchar(64),
    sort            int          NOT NULL DEFAULT 0,
    enabled         boolean      NOT NULL DEFAULT true,
    config_json     jsonb        NOT NULL DEFAULT '{}'::jsonb,
    created_at      timestamptz  NOT NULL DEFAULT now(),
    updated_at      timestamptz  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_meta_tab ON meta_tab (entity_code, code);
CREATE INDEX idx_meta_tab_entity ON meta_tab (entity_code, sort);

INSERT INTO meta_tab (id, entity_code, code, name, tab_type, permission_code, sort) VALUES
    (901001, 'PdmDoc', 'props',     '属性',     'PROPS',     'pdm.doc.read', 10),
    (901002, 'PdmDoc', 'files',     '文件',     'FILES',     'pdm.doc.read', 20),
    (901003, 'PdmDoc', 'versions',  '历史版本', 'VERSIONS',  'pdm.doc.read', 30),
    (901004, 'PdmDoc', 'relations', '关联对象', 'RELATIONS', 'pdm.doc.read', 40),
    (901005, 'PdmDoc', 'workflow',  '流程',     'WORKFLOW',  'pdm.doc.read', 50),
    (901006, 'PdmDoc', 'acl',       '权限',     'ACL',       'pdm.doc.read', 60),
    (901007, 'PdmDoc', 'logs',      '日志',     'LOGS',      'pdm.doc.read', 70)
ON CONFLICT DO NOTHING;
