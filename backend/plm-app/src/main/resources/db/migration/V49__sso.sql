-- =====================================================================
-- P9-5 单点登录：OIDC + LDAP/AD 身份提供方 + 账号绑定
--
-- 为什么单开 sys_sso_provider 而不是复用 sys_user.auth_provider：
--   auth_provider 只记录"这个账号从哪来"，而 IdP 的地址/客户端/密钥/属性映射
--   是一份可维护的配置，且要支持同时接入多个（域账号 + 统一身份平台）。
--
-- 为什么单开 sys_user_sso 而不是给 sys_user 加 sso_subject 列：
--   一个人可能同时绑定多个 IdP，且 subject 只在某个 IdP 内唯一，
--   放 user 表上会退化成"只能绑一个"，换 IdP 时还得改用户表。
--
-- config 里的密钥字段（OIDC clientSecret / LDAP bindCredential）只写不读回，
-- 与 P9-1 端点认证、P9-4 Webhook 密钥同一口径。
-- =====================================================================

CREATE TABLE sys_sso_provider (
    id              bigint       PRIMARY KEY,
    company_id      bigint       NOT NULL,
    code            varchar(64)  NOT NULL,
    name            varchar(200) NOT NULL,
    type            varchar(16)  NOT NULL,
    enabled         boolean      NOT NULL DEFAULT true,
    config          jsonb        NOT NULL DEFAULT '{}'::jsonb,
    auto_provision  boolean      NOT NULL DEFAULT true,
    default_role_code varchar(64),
    remark          varchar(1000),
    created_by      bigint,
    created_at      timestamptz  NOT NULL DEFAULT now(),
    updated_by      bigint,
    updated_at      timestamptz  NOT NULL DEFAULT now(),
    deleted         boolean      NOT NULL DEFAULT false,
    CONSTRAINT ck_sys_sso_provider_type CHECK (type IN ('OIDC', 'LDAP'))
);
CREATE UNIQUE INDEX uk_sys_sso_provider_code
    ON sys_sso_provider (company_id, code) WHERE deleted = false;

CREATE TABLE sys_user_sso (
    id                bigint       PRIMARY KEY,
    user_id           bigint       NOT NULL,
    provider_id       bigint       NOT NULL,
    -- IdP 内的稳定主体标识（OIDC 的 sub / LDAP 的 objectGUID 或 entryDN）
    subject           varchar(200) NOT NULL,
    external_username varchar(128),
    last_login_at     timestamptz,
    created_at        timestamptz  NOT NULL DEFAULT now(),
    updated_at        timestamptz  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_sys_user_sso_subject ON sys_user_sso (provider_id, subject);
CREATE UNIQUE INDEX uk_sys_user_sso_user ON sys_user_sso (provider_id, user_id);
CREATE INDEX idx_sys_user_sso_user ON sys_user_sso (user_id);

-- 只授给系统管理员：这份配置含 IdP 客户端密钥 / 域服务账号，不该给业务角色
INSERT INTO sys_permission (code, module, action, name, name_en, perm_type) VALUES
    ('sys.sso.read',  'IAM', 'READ',  '单点登录配置查看', 'SSO provider read',  'FUNCTION'),
    ('sys.sso.write', 'IAM', 'WRITE', '单点登录配置维护', 'SSO provider write', 'FUNCTION')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission (role_id, permission_code)
SELECT r.id, p.code
FROM sys_role r
JOIN sys_permission p ON p.code IN ('sys.sso.read', 'sys.sso.write')
WHERE r.code = 'SYS_ADMIN'
ON CONFLICT DO NOTHING;
