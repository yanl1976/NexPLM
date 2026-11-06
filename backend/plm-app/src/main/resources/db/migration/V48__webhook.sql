-- =====================================================================
-- P9-4 Webhook：事件订阅（文档发布 / 变更生效 / 任务完成）+ 签名 + 重试
--
-- 为什么订阅与投递分两张表：
--   订阅是可编辑的配置（URL/密钥/事件范围）；投递是一次性事实，必须保留原文快照。
--   两者混一张表，改一次 URL 就会把历史投递的"发到哪、发了什么"一起改掉，排障时无从复原。
--
-- 为什么 payload 存 jsonb 而不是每次重算：
--   重放（replay）要能发出与当初完全一致的报文，否则对端会看到"同一事件两种内容"。
--
-- 签名：HMAC-SHA256(secret, 原始报文) 放 X-NexPLM-Signature，对端可独立校验；
--   secret 只写不读回，与 P9-1 端点认证一致。
-- =====================================================================

CREATE TABLE itg_webhook (
    id           bigint       PRIMARY KEY,
    company_id   bigint       NOT NULL,
    code         varchar(64)  NOT NULL,
    name         varchar(200) NOT NULL,
    url          varchar(500) NOT NULL,
    secret       varchar(200) NOT NULL,
    events       jsonb        NOT NULL DEFAULT '[]'::jsonb,
    enabled      boolean      NOT NULL DEFAULT true,
    max_attempts int          NOT NULL DEFAULT 5,
    timeout_ms   int          NOT NULL DEFAULT 5000,
    remark       varchar(1000),
    created_by   bigint,
    created_at   timestamptz  NOT NULL DEFAULT now(),
    updated_by   bigint,
    updated_at   timestamptz  NOT NULL DEFAULT now(),
    deleted      boolean      NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_itg_webhook_code
    ON itg_webhook (company_id, code) WHERE deleted = false;

CREATE TABLE itg_webhook_delivery (
    id            bigint       PRIMARY KEY,
    company_id    bigint       NOT NULL,
    webhook_id    bigint       NOT NULL,
    event_type    varchar(64)  NOT NULL,
    -- 业务幂等键：同一订阅下同一事件只投递一次（重试复用本行，不新建）
    event_key     varchar(200) NOT NULL,
    payload       jsonb        NOT NULL,
    status        varchar(16)  NOT NULL DEFAULT 'PENDING',
    attempt       int          NOT NULL DEFAULT 0,
    max_attempts  int          NOT NULL DEFAULT 5,
    next_retry_at timestamptz,
    http_status   int,
    response_body varchar(2000),
    error_msg     varchar(1000),
    duration_ms   int,
    signature     varchar(128),
    started_at    timestamptz,
    finished_at   timestamptz,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    updated_at    timestamptz  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_itg_webhook_delivery
    ON itg_webhook_delivery (webhook_id, event_key);
CREATE INDEX idx_itg_webhook_delivery_due
    ON itg_webhook_delivery (status, next_retry_at);
CREATE INDEX idx_itg_webhook_delivery_list
    ON itg_webhook_delivery (company_id, webhook_id, created_at DESC);

INSERT INTO sys_permission (code, module, action, name, name_en, perm_type) VALUES
    ('itg.webhook.read',  'ITG', 'READ',  'Webhook 订阅查看', 'Webhook read',  'FUNCTION'),
    ('itg.webhook.write', 'ITG', 'WRITE', 'Webhook 订阅维护', 'Webhook write', 'FUNCTION')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission (role_id, permission_code)
SELECT r.id, p.code
FROM sys_role r
JOIN sys_permission p ON p.code IN ('itg.webhook.read', 'itg.webhook.write')
WHERE r.code IN ('SYS_ADMIN', 'BUSINESS')
ON CONFLICT DO NOTHING;
