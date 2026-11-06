-- =====================================================================
-- P9-3 开放平台：API Key + 作用域 + 限流
--
-- 为什么不用"给外部一个长期 JWT"：
--   JWT 是无状态的，签发后到过期前收不回来；外部集成的 key 泄漏时必须能立刻失效。
--   所以 API Key 走库校验（enabled/expires_at 改一下立即生效），且只存 hash。
--
-- 为什么存 key_prefix：
--   校验时要用某个值定位到一行，否则就得全表逐行比对 hash（O(n) 的 bcrypt 级开销）。
--   prefix 只是索引线索、不是秘密；真正的凭证是 key 全文，服务端只保留 sha256。
--
-- 作用域（scopes）映射成权限字面量 open.<scope>，由 @PreAuthorize 判定，
-- 与站内权限走同一套机制，不再自造一套鉴权。
--
-- 限流窗口在内存里按 client 计数（单机部署够用）；多实例部署时需换成 Redis，
-- 这一点在文档遗留里注明，不在本表里假装解决。
-- =====================================================================

CREATE TABLE itg_api_client (
    id             bigint       PRIMARY KEY,
    company_id     bigint       NOT NULL,
    code           varchar(64)  NOT NULL,
    name           varchar(200) NOT NULL,
    key_prefix     varchar(16)  NOT NULL,
    key_hash       varchar(64)  NOT NULL,
    scopes         jsonb        NOT NULL DEFAULT '[]'::jsonb,
    rate_limit_per_min int      NOT NULL DEFAULT 60,
    enabled        boolean      NOT NULL DEFAULT true,
    expires_at     timestamptz,
    last_used_at   timestamptz,
    remark         varchar(1000),
    created_by     bigint,
    created_at     timestamptz  NOT NULL DEFAULT now(),
    updated_by     bigint,
    updated_at     timestamptz  NOT NULL DEFAULT now(),
    deleted        boolean      NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_itg_api_client_code
    ON itg_api_client (company_id, code) WHERE deleted = false;
CREATE UNIQUE INDEX uk_itg_api_client_prefix
    ON itg_api_client (key_prefix) WHERE deleted = false;

INSERT INTO sys_permission (code, module, action, name, name_en, perm_type) VALUES
    ('itg.client.read',   'ITG', 'READ',  'API 客户端查看', 'API client read',   'FUNCTION'),
    ('itg.client.write',  'ITG', 'WRITE', 'API 客户端维护', 'API client write',  'FUNCTION')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission (role_id, permission_code)
SELECT r.id, p.code
FROM sys_role r
JOIN sys_permission p ON p.code IN ('itg.client.read', 'itg.client.write')
WHERE r.code IN ('SYS_ADMIN', 'BUSINESS')
ON CONFLICT DO NOTHING;
