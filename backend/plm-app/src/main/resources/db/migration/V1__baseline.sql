-- =====================================================================
-- NexPLM 基线结构（PostgreSQL 16）
-- 约定：业务主键 bigint（应用侧雪花 ID）；时间 timestamptz；扩展属性 jsonb；
--      业务表带 version（乐观锁）与审计四列；审计/日志表不逻辑删除。
-- =====================================================================

-- ---------- 1. 组织与身份 ----------
CREATE TABLE sys_company (
    id          bigint       PRIMARY KEY,
    code        varchar(64)  NOT NULL,
    name        varchar(200) NOT NULL,
    short_name  varchar(100),
    erp_code    varchar(64),
    status      varchar(16)  NOT NULL DEFAULT 'ACTIVE',
    created_by  bigint,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_by  bigint,
    updated_at  timestamptz  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_sys_company_code ON sys_company (code);

CREATE TABLE sys_org (
    id          bigint       PRIMARY KEY,
    company_id  bigint       NOT NULL,
    parent_id   bigint,
    path        varchar(500) NOT NULL DEFAULT '/',
    code        varchar(64)  NOT NULL,
    name        varchar(200) NOT NULL,
    org_type    varchar(32)  NOT NULL DEFAULT 'DEPT',
    sort        int          NOT NULL DEFAULT 0,
    leader_id   bigint,
    status      varchar(16)  NOT NULL DEFAULT 'ACTIVE',
    ext_json    jsonb        NOT NULL DEFAULT '{}'::jsonb,
    created_by  bigint,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_by  bigint,
    updated_at  timestamptz  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_sys_org_company_code ON sys_org (company_id, code);
CREATE INDEX idx_sys_org_path ON sys_org (path);
CREATE INDEX idx_sys_org_parent ON sys_org (parent_id);

CREATE TABLE sys_user (
    id              bigint       PRIMARY KEY,
    company_id      bigint       NOT NULL,
    username        varchar(64)  NOT NULL,
    password_hash   varchar(200) NOT NULL,
    display_name    varchar(100) NOT NULL,
    email           varchar(128),
    mobile          varchar(32),
    employee_no     varchar(64),
    security_level  smallint     NOT NULL DEFAULT 0,
    status          varchar(16)  NOT NULL DEFAULT 'ACTIVE',
    must_change_pwd boolean      NOT NULL DEFAULT true,
    last_login_at   timestamptz,
    pwd_updated_at  timestamptz,
    lock_until      timestamptz,
    auth_provider   varchar(16)  NOT NULL DEFAULT 'LOCAL',
    ext_json        jsonb        NOT NULL DEFAULT '{}'::jsonb,
    version         int          NOT NULL DEFAULT 0,
    deleted         boolean      NOT NULL DEFAULT false,
    created_by      bigint,
    created_at      timestamptz  NOT NULL DEFAULT now(),
    updated_by      bigint,
    updated_at      timestamptz  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_sys_user_username ON sys_user (username) WHERE deleted = false;
CREATE INDEX idx_sys_user_company ON sys_user (company_id, status);

CREATE TABLE sys_user_org (
    user_id    bigint  NOT NULL,
    org_id     bigint  NOT NULL,
    is_primary boolean NOT NULL DEFAULT false,
    PRIMARY KEY (user_id, org_id)
);

CREATE TABLE sys_group (
    id          bigint       PRIMARY KEY,
    company_id  bigint       NOT NULL,
    org_id      bigint,
    code        varchar(64)  NOT NULL,
    name        varchar(200) NOT NULL,
    group_type  varchar(32)  NOT NULL DEFAULT 'DEPT',
    status      varchar(16)  NOT NULL DEFAULT 'ACTIVE',
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_at  timestamptz  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_sys_group_company_code ON sys_group (company_id, code);

CREATE TABLE sys_user_group (
    user_id    bigint  NOT NULL,
    group_id   bigint  NOT NULL,
    is_primary boolean NOT NULL DEFAULT false,
    PRIMARY KEY (user_id, group_id)
);

CREATE TABLE sys_role (
    id          bigint       PRIMARY KEY,
    company_id  bigint       NOT NULL,
    code        varchar(64)  NOT NULL,
    name        varchar(200) NOT NULL,
    role_type   varchar(32)  NOT NULL DEFAULT 'BUSINESS',
    data_scope  varchar(32)  NOT NULL DEFAULT 'COMPANY',
    status      varchar(16)  NOT NULL DEFAULT 'ACTIVE',
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_at  timestamptz  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_sys_role_company_code ON sys_role (company_id, code);

CREATE TABLE sys_user_role (
    user_id bigint NOT NULL,
    role_id bigint NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE sys_group_role (
    group_id bigint NOT NULL,
    role_id  bigint NOT NULL,
    PRIMARY KEY (group_id, role_id)
);

CREATE TABLE sys_org_isolate (
    id                 bigint      PRIMARY KEY,
    group_id           bigint      NOT NULL,
    isolated_group_id  bigint      NOT NULL,
    mode               varchar(16) NOT NULL DEFAULT 'BOTH',
    created_at         timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_sys_org_isolate ON sys_org_isolate (group_id, isolated_group_id);

CREATE TABLE sys_org_share (
    id                bigint      PRIMARY KEY,
    group_id          bigint      NOT NULL,
    shared_group_id   bigint      NOT NULL,
    mode              varchar(16) NOT NULL DEFAULT 'READ',
    created_at        timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_sys_org_share ON sys_org_share (group_id, shared_group_id);

-- ---------- 2. 权限 ----------
CREATE TABLE sys_permission (
    code        varchar(128) PRIMARY KEY,
    module      varchar(32)  NOT NULL,
    action      varchar(64)  NOT NULL,
    name        varchar(200) NOT NULL,
    name_en     varchar(200),
    perm_type   varchar(16)  NOT NULL DEFAULT 'FUNCTION',
    remark      varchar(500)
);

CREATE TABLE sys_role_permission (
    role_id         bigint       NOT NULL,
    permission_code varchar(128) NOT NULL,
    PRIMARY KEY (role_id, permission_code)
);

CREATE TABLE sys_acl_entry (
    id               bigint      PRIMARY KEY,
    object_id        bigint      NOT NULL,
    actor_type       varchar(16) NOT NULL,
    actor_id         bigint,
    actions          varchar(200) NOT NULL,
    effect           varchar(8)  NOT NULL DEFAULT 'ALLOW',
    expire_at        timestamptz,
    source           varchar(16) NOT NULL DEFAULT 'MANUAL',
    source_object_id bigint,
    created_by       bigint,
    created_at       timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_sys_acl_entry ON sys_acl_entry (object_id, actor_type, coalesce(actor_id, 0), coalesce(source_object_id, 0));
CREATE INDEX idx_sys_acl_object ON sys_acl_entry (object_id);

CREATE TABLE sys_permission_rule (
    id          bigint       PRIMARY KEY,
    code        varchar(64)  NOT NULL,
    name        varchar(200) NOT NULL,
    object_type varchar(64)  NOT NULL,
    expr_json   jsonb        NOT NULL,
    actions     varchar(200) NOT NULL,
    priority    int          NOT NULL DEFAULT 100,
    effect      varchar(8)   NOT NULL DEFAULT 'ALLOW',
    enabled     boolean      NOT NULL DEFAULT true,
    remark      varchar(500),
    created_by  bigint,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_at  timestamptz  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_sys_permission_rule_code ON sys_permission_rule (code);

-- ---------- 3. 元数据 ----------
CREATE TABLE meta_entity (
    id                bigint       PRIMARY KEY,
    code              varchar(64)  NOT NULL,
    name              varchar(200) NOT NULL,
    name_en           varchar(200),
    table_name        varchar(64)  NOT NULL DEFAULT 'plm_object',
    parent_code       varchar(64),
    abstract_flag     boolean      NOT NULL DEFAULT false,
    capabilities      jsonb        NOT NULL DEFAULT '[]'::jsonb,
    version_policy    varchar(32)  NOT NULL DEFAULT 'LETTER',
    iteration_policy  varchar(32)  NOT NULL DEFAULT 'NUMBER',
    lifecycle_code    varchar(64),
    phase_code        varchar(64),
    workflow_allowed  boolean      NOT NULL DEFAULT false,
    security_level    smallint     NOT NULL DEFAULT 0,
    schema_json       jsonb        NOT NULL DEFAULT '{}'::jsonb,
    version_no        int          NOT NULL DEFAULT 1,
    status            varchar(16)  NOT NULL DEFAULT 'DRAFT',
    remark            varchar(500),
    created_at        timestamptz  NOT NULL DEFAULT now(),
    updated_at        timestamptz  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_meta_entity_code ON meta_entity (code);
CREATE INDEX idx_meta_entity_parent ON meta_entity (parent_code);

CREATE TABLE meta_field (
    id             bigint       PRIMARY KEY,
    entity_code    varchar(64)  NOT NULL,
    code           varchar(64)  NOT NULL,
    name           varchar(200) NOT NULL,
    name_en        varchar(200),
    value_type     varchar(48)  NOT NULL,
    data_type      varchar(32)  NOT NULL,
    length         int,
    scale          int,
    required       boolean      NOT NULL DEFAULT false,
    default_value  varchar(500),
    enum_code      varchar(64),
    ref_entity     varchar(64),
    ref_field      varchar(64),
    ui_json        jsonb        NOT NULL DEFAULT '{}'::jsonb,
    security_level smallint     NOT NULL DEFAULT 0,
    indexed        boolean      NOT NULL DEFAULT false,
    searchable     boolean      NOT NULL DEFAULT false,
    sort           int          NOT NULL DEFAULT 0,
    version_no     int          NOT NULL DEFAULT 1,
    status         varchar(16)  NOT NULL DEFAULT 'DRAFT',
    created_at     timestamptz  NOT NULL DEFAULT now(),
    updated_at     timestamptz  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_meta_field ON meta_field (entity_code, code);

CREATE TABLE meta_relation_type (
    id             bigint       PRIMARY KEY,
    code           varchar(64)  NOT NULL,
    name           varchar(200) NOT NULL,
    src_entity     varchar(64)  NOT NULL,
    dst_entity     varchar(64)  NOT NULL,
    link_table     varchar(64),
    contain_sub    boolean      NOT NULL DEFAULT false,
    loop_check     boolean      NOT NULL DEFAULT true,
    seq_step       int          NOT NULL DEFAULT 10,
    attrs_schema   jsonb        NOT NULL DEFAULT '{}'::jsonb,
    capabilities   jsonb        NOT NULL DEFAULT '[]'::jsonb,
    status         varchar(16)  NOT NULL DEFAULT 'DRAFT',
    created_at     timestamptz  NOT NULL DEFAULT now(),
    updated_at     timestamptz  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_meta_relation_type_code ON meta_relation_type (code);

CREATE TABLE meta_enum (
    code       varchar(64)  PRIMARY KEY,
    name       varchar(200) NOT NULL,
    status     varchar(16)  NOT NULL DEFAULT 'ACTIVE',
    created_at timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE meta_enum_item (
    enum_code   varchar(64)  NOT NULL,
    item_code   varchar(64)  NOT NULL,
    item_name   varchar(200) NOT NULL,
    item_name_en varchar(200),
    sort        int          NOT NULL DEFAULT 0,
    ext_json    jsonb        NOT NULL DEFAULT '{}'::jsonb,
    status      varchar(16)  NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (enum_code, item_code)
);

CREATE TABLE meta_form (
    id          bigint      PRIMARY KEY,
    code        varchar(64) NOT NULL,
    entity_code varchar(64) NOT NULL,
    page_type   varchar(16) NOT NULL,
    layout_json jsonb       NOT NULL DEFAULT '{}'::jsonb,
    version_no  int         NOT NULL DEFAULT 1,
    status      varchar(16) NOT NULL DEFAULT 'DRAFT',
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_meta_form ON meta_form (entity_code, page_type, code);

CREATE TABLE meta_menu (
    id              bigint       PRIMARY KEY,
    parent_id       bigint,
    code            varchar(128) NOT NULL,
    title           varchar(200) NOT NULL,
    route           varchar(300),
    icon            varchar(100),
    sort            int          NOT NULL DEFAULT 0,
    permission_code varchar(128),
    visible         boolean      NOT NULL DEFAULT true,
    created_at      timestamptz  NOT NULL DEFAULT now(),
    updated_at      timestamptz  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_meta_menu_code ON meta_menu (code);

CREATE TABLE meta_lifecycle (
    id              bigint       PRIMARY KEY,
    code            varchar(64)  NOT NULL,
    name            varchar(200) NOT NULL,
    type            varchar(16)  NOT NULL DEFAULT 'MAIN',
    definition_json jsonb        NOT NULL,
    version_no      int          NOT NULL DEFAULT 1,
    status          varchar(16)  NOT NULL DEFAULT 'DRAFT',
    created_at      timestamptz  NOT NULL DEFAULT now(),
    updated_at      timestamptz  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_meta_lifecycle_code ON meta_lifecycle (code);

-- ---------- 4. 业务对象通用层 ----------
CREATE TABLE plm_object (
    id              bigint       PRIMARY KEY,
    object_type     varchar(64)  NOT NULL,
    master_id       bigint       NOT NULL,
    code            varchar(128) NOT NULL,
    name            varchar(500) NOT NULL,
    company_id      bigint       NOT NULL,
    org_id          bigint,
    owner_id        bigint,
    owner_group_id  bigint,
    security_level  smallint     NOT NULL DEFAULT 0,
    lifecycle_state varchar(32)  NOT NULL,
    phase_state     varchar(32),
    workflow_state  varchar(16)  NOT NULL DEFAULT 'NONE',
    revision        int          NOT NULL DEFAULT 1,
    iteration       int          NOT NULL DEFAULT 1,
    is_latest       boolean      NOT NULL DEFAULT true,
    released_at     timestamptz,
    obsolete_at     timestamptz,
    ext_json        jsonb        NOT NULL DEFAULT '{}'::jsonb,
    version         int          NOT NULL DEFAULT 0,
    deleted         boolean      NOT NULL DEFAULT false,
    created_by      bigint,
    created_at      timestamptz  NOT NULL DEFAULT now(),
    updated_by      bigint,
    updated_at      timestamptz  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_plm_object_code ON plm_object (object_type, code, revision, iteration) WHERE deleted = false;
CREATE INDEX idx_plm_object_list ON plm_object (object_type, company_id, is_latest, deleted);
CREATE INDEX idx_plm_object_master ON plm_object (master_id);
CREATE INDEX idx_plm_object_code_only ON plm_object (code);
CREATE INDEX idx_plm_object_owner ON plm_object (owner_id);
CREATE INDEX idx_plm_object_updated ON plm_object (updated_at DESC);
CREATE INDEX idx_plm_object_ext ON plm_object USING gin (ext_json jsonb_path_ops);

CREATE TABLE plm_relation (
    id            bigint        PRIMARY KEY,
    relation_type varchar(64)   NOT NULL,
    src_id        bigint        NOT NULL,
    dst_id        bigint        NOT NULL,
    seq           int           NOT NULL DEFAULT 0,
    qty           numeric(18,6),
    attrs_json    jsonb         NOT NULL DEFAULT '{}'::jsonb,
    company_id    bigint        NOT NULL,
    deleted       boolean       NOT NULL DEFAULT false,
    created_by    bigint,
    created_at    timestamptz   NOT NULL DEFAULT now(),
    updated_by    bigint,
    updated_at    timestamptz   NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_plm_relation ON plm_relation (relation_type, src_id, dst_id, seq) WHERE deleted = false;
CREATE INDEX idx_plm_relation_src ON plm_relation (relation_type, src_id);
CREATE INDEX idx_plm_relation_dst ON plm_relation (relation_type, dst_id);

CREATE TABLE plm_checkout (
    id             bigint       PRIMARY KEY,
    object_id      bigint       NOT NULL,
    user_id        bigint       NOT NULL,
    checked_out_at timestamptz  NOT NULL DEFAULT now(),
    expire_at      timestamptz,
    client_ip      varchar(64),
    machine        varchar(200),
    remark         varchar(500)
);
CREATE UNIQUE INDEX uk_plm_checkout_object ON plm_checkout (object_id);

CREATE TABLE plm_state_log (
    id               bigint      PRIMARY KEY,
    object_id        bigint      NOT NULL,
    state_type       varchar(16) NOT NULL,
    from_state       varchar(32),
    to_state         varchar(32),
    action           varchar(64),
    reason           varchar(500),
    operator_id      bigint,
    flow_instance_id varchar(64),
    created_at       timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_plm_state_log_object ON plm_state_log (object_id, created_at DESC);

-- ---------- 5. 文件 ----------
CREATE TABLE file_object (
    id          bigint       PRIMARY KEY,
    sha256      char(64)     NOT NULL,
    size        bigint       NOT NULL,
    mime        varchar(128),
    ext         varchar(32),
    bucket      varchar(64)  NOT NULL,
    object_key  varchar(500) NOT NULL,
    ref_count   int          NOT NULL DEFAULT 0,
    scan_status varchar(16)  NOT NULL DEFAULT 'PENDING',
    created_at  timestamptz  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_file_object_sha ON file_object (sha256);

CREATE TABLE file_link (
    id              bigint       PRIMARY KEY,
    file_id         bigint       NOT NULL,
    owner_object_id bigint       NOT NULL,
    version_no      int          NOT NULL DEFAULT 1,
    file_role       varchar(16)  NOT NULL DEFAULT 'ATTACHMENT',
    file_name       varchar(255) NOT NULL,
    is_latest       boolean      NOT NULL DEFAULT true,
    sort            int          NOT NULL DEFAULT 0,
    created_by      bigint,
    created_at      timestamptz  NOT NULL DEFAULT now()
);
CREATE INDEX idx_file_link_owner ON file_link (owner_object_id, file_role);
CREATE INDEX idx_file_link_file ON file_link (file_id);

-- ---------- 6. 编码 ----------
CREATE TABLE code_rule (
    id            bigint       PRIMARY KEY,
    code          varchar(64)  NOT NULL,
    name          varchar(200) NOT NULL,
    segments_json jsonb        NOT NULL,
    scope_json    jsonb        NOT NULL DEFAULT '{}'::jsonb,
    reset_policy  varchar(16)  NOT NULL DEFAULT 'NONE',
    status        varchar(16)  NOT NULL DEFAULT 'ACTIVE',
    version_no    int          NOT NULL DEFAULT 1,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    updated_at    timestamptz  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_code_rule_code ON code_rule (code);

CREATE TABLE code_sequence (
    id         bigint      PRIMARY KEY,
    rule_code  varchar(64) NOT NULL,
    scope_key  varchar(64) NOT NULL DEFAULT '',
    current_no bigint      NOT NULL DEFAULT 0,
    version    int         NOT NULL DEFAULT 0,
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_code_sequence ON code_sequence (rule_code, scope_key);

CREATE TABLE code_history (
    id         bigint       PRIMARY KEY,
    rule_code  varchar(64)  NOT NULL,
    scope_key  varchar(64)  NOT NULL DEFAULT '',
    code_value varchar(128) NOT NULL,
    object_id  bigint,
    state      varchar(16)  NOT NULL DEFAULT 'USED',
    created_by bigint,
    created_at timestamptz  NOT NULL DEFAULT now()
);
CREATE INDEX idx_code_history_value ON code_history (code_value);

-- ---------- 7. 审计 ----------
CREATE TABLE sys_audit_log (
    id          bigserial    PRIMARY KEY,
    trace_id    varchar(64),
    user_id     bigint,
    org_id      bigint,
    module      varchar(32)  NOT NULL,
    action      varchar(64)  NOT NULL,
    object_type varchar(64),
    object_id   bigint,
    result      varchar(16)  NOT NULL DEFAULT 'SUCCESS',
    detail      varchar(4000),
    before_json jsonb,
    after_json  jsonb,
    ip          varchar(64),
    ua          varchar(500),
    prev_hash   char(64),
    hash        char(64),
    created_at  timestamptz  NOT NULL DEFAULT now()
);
CREATE INDEX idx_sys_audit_log_user ON sys_audit_log (user_id, created_at DESC);
CREATE INDEX idx_sys_audit_log_object ON sys_audit_log (object_type, object_id);
CREATE INDEX idx_sys_audit_log_time ON sys_audit_log (created_at DESC);

CREATE TABLE sys_login_log (
    id          bigserial   PRIMARY KEY,
    username    varchar(64) NOT NULL,
    user_id     bigint,
    result      varchar(16) NOT NULL,
    fail_reason varchar(200),
    ip          varchar(64),
    ua          varchar(500),
    created_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_sys_login_log_time ON sys_login_log (created_at DESC);
