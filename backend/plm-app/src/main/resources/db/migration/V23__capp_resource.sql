-- =====================================================================
-- P6-3 工艺资源库（P6 任务 3）
--
-- 三张表 + 一个引用列：
--   capp_resource_class  资源分类树（parent_id 自关联，NULL 为根）
--   capp_resource        资源主数据（刀具/量具/设备/辅料…），动态属性放 attrs jsonb
--   capp_resource_query  保存的查询条件（分类 + 关键字 + 动态属性组合，可复用）
--   capp_routing_step.resource_id  工序引用的资源（引用统计的口径）
--
-- 动态属性约定：attrs 为 {属性名: 值} 的扁平 jsonb（值统一存字符串），
--   查询用 jsonb 包含匹配（attrs @> '{"品牌":"XX"}'），避免为每个属性建列。
-- =====================================================================

CREATE TABLE capp_resource_class (
    id         bigint       PRIMARY KEY,
    company_id bigint       NOT NULL,
    parent_id  bigint,                                       -- NULL = 根分类
    code       varchar(64)  NOT NULL,
    name       varchar(200) NOT NULL,
    sort_no    int          NOT NULL DEFAULT 0,
    remark     varchar(500),
    created_at timestamptz  NOT NULL DEFAULT now(),
    updated_at timestamptz  NOT NULL DEFAULT now(),
    deleted    boolean      NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_capp_resource_class_code ON capp_resource_class (company_id, code) WHERE deleted = false;
CREATE INDEX idx_capp_resource_class_parent ON capp_resource_class (company_id, parent_id) WHERE deleted = false;

CREATE TABLE capp_resource (
    id         bigint       PRIMARY KEY,
    company_id bigint       NOT NULL,
    class_id   bigint       NOT NULL,
    code       varchar(64)  NOT NULL,
    name       varchar(200) NOT NULL,
    spec       varchar(500),
    unit       varchar(32),
    status     varchar(16)  NOT NULL DEFAULT 'ACTIVE',       -- ACTIVE / DISABLED
    attrs      jsonb        NOT NULL DEFAULT '{}'::jsonb,
    remark     varchar(500),
    created_by bigint,
    created_at timestamptz  NOT NULL DEFAULT now(),
    updated_by bigint,
    updated_at timestamptz  NOT NULL DEFAULT now(),
    deleted    boolean      NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_capp_resource_code ON capp_resource (company_id, code) WHERE deleted = false;
CREATE INDEX idx_capp_resource_class ON capp_resource (company_id, class_id) WHERE deleted = false;
CREATE INDEX idx_capp_resource_attrs ON capp_resource USING gin (attrs);

CREATE TABLE capp_resource_query (
    id             bigint       PRIMARY KEY,
    company_id     bigint       NOT NULL,
    name           varchar(100) NOT NULL,
    class_id       bigint,
    condition_json jsonb        NOT NULL DEFAULT '{}'::jsonb,
    created_by     bigint,
    created_at     timestamptz  NOT NULL DEFAULT now(),
    updated_at     timestamptz  NOT NULL DEFAULT now(),
    deleted        boolean      NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_capp_resource_query_name ON capp_resource_query (company_id, name) WHERE deleted = false;

-- 工序引用资源（引用统计与后续卡片/成册用）
ALTER TABLE capp_routing_step ADD COLUMN IF NOT EXISTS resource_id bigint;
CREATE INDEX IF NOT EXISTS idx_capp_step_resource ON capp_routing_step (resource_id);

-- ---------------------------------------------------------------------
-- 权限点
-- ---------------------------------------------------------------------
INSERT INTO sys_permission (code, module, action, name, name_en, perm_type) VALUES
    ('capp.resource.read',  'CAPP', 'READ',  '工艺资源查看', 'Resource read',  'FUNCTION'),
    ('capp.resource.write', 'CAPP', 'WRITE', '工艺资源维护', 'Resource write', 'FUNCTION')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission (role_id, permission_code)
SELECT r.id, p.code
  FROM sys_role r
  JOIN sys_permission p ON p.code LIKE 'capp.resource.%'
 WHERE r.code IN ('SYS_ADMIN', 'BUSINESS')
ON CONFLICT DO NOTHING;
