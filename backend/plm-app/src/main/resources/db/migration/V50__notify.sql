-- =====================================================================
-- P9-6 通知通道（SPI）：邮件 / 企业微信 / 钉钉
--
-- 为什么通道与消息分表：
--   通道是可改的配置（SMTP 口令、机器人 webhook），消息是一次性事实（发了什么、结果如何）。
--   与 P9-4 Webhook 同一取舍：改配置不能改写历史投递记录。
--
-- 为什么消息存渲染后的标题/正文快照：
--   重发要能发出与当初一致的文案，而不是按"当前"数据重新渲染——否则对端会收到两份不同内容。
--
-- 邮件走 SMTP：host/port/username/password/from 等字段在通道配置里可填，
-- 留空时回落 application.yml 的 nexplm.notify.email.* 占位符；
-- 两处都没填 → 明确报「SMTP 未配置」，不静默丢弃。
-- =====================================================================

CREATE TABLE sys_notify_channel (
    id           bigint       PRIMARY KEY,
    company_id   bigint       NOT NULL,
    code         varchar(64)  NOT NULL,
    name         varchar(200) NOT NULL,
    type         varchar(16)  NOT NULL,
    enabled      boolean      NOT NULL DEFAULT true,
    events       jsonb        NOT NULL DEFAULT '[]'::jsonb,
    config       jsonb        NOT NULL DEFAULT '{}'::jsonb,
    -- 该通道的发送失败重试上限：落到每条消息的 max_attempts，避免所有通道共用一个硬编码值
    max_attempts int          NOT NULL DEFAULT 3,
    remark       varchar(1000),
    created_by   bigint,
    created_at   timestamptz  NOT NULL DEFAULT now(),
    updated_by   bigint,
    updated_at   timestamptz  NOT NULL DEFAULT now(),
    deleted      boolean      NOT NULL DEFAULT false,
    CONSTRAINT ck_sys_notify_channel_type CHECK (type IN ('EMAIL', 'WECOM', 'DINGTALK'))
);
CREATE UNIQUE INDEX uk_sys_notify_channel_code
    ON sys_notify_channel (company_id, code) WHERE deleted = false;

CREATE TABLE sys_notify_message (
    id            bigint       PRIMARY KEY,
    company_id    bigint       NOT NULL,
    channel_id    bigint       NOT NULL,
    event_type    varchar(64)  NOT NULL,
    -- 幂等键：同一通道下同一事件只发一次（重试复用本行，不新建）
    event_key     varchar(200) NOT NULL,
    title         varchar(300) NOT NULL,
    content       varchar(4000) NOT NULL,
    status        varchar(16)  NOT NULL DEFAULT 'PENDING',
    attempt       int          NOT NULL DEFAULT 0,
    max_attempts  int          NOT NULL DEFAULT 3,
    next_retry_at timestamptz,
    error_msg     varchar(1000),
    response      varchar(2000),
    started_at    timestamptz,
    finished_at   timestamptz,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    updated_at    timestamptz  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_sys_notify_message
    ON sys_notify_message (channel_id, event_key);
CREATE INDEX idx_sys_notify_message_due
    ON sys_notify_message (status, next_retry_at);
CREATE INDEX idx_sys_notify_message_list
    ON sys_notify_message (company_id, channel_id, created_at DESC);

-- 通知配置含 SMTP 口令/机器人密钥，只授系统管理员
INSERT INTO sys_permission (code, module, action, name, name_en, perm_type) VALUES
    ('sys.notify.read',  'IAM', 'READ',  '通知通道查看', 'Notify channel read',  'FUNCTION'),
    ('sys.notify.write', 'IAM', 'WRITE', '通知通道维护', 'Notify channel write', 'FUNCTION')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission (role_id, permission_code)
SELECT r.id, p.code
FROM sys_role r
JOIN sys_permission p ON p.code IN ('sys.notify.read', 'sys.notify.write')
WHERE r.code = 'SYS_ADMIN'
ON CONFLICT DO NOTHING;
