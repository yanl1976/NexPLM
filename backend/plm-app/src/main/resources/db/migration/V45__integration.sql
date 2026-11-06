-- =====================================================================
-- P9-1 集成与开放平台·集成框架
--
-- 四张表：端点（发给谁）/ 映射（字段怎么转）/ 任务（发了一次什么）/ 日志（每次尝试）。
--
-- 为什么任务要落表而不是直接调 HTTP：
--   对端系统会超时、会 500、会重复收到；没有任务表就无法回答"这条到底发没发、
--   发了几次、对端回了什么"，也没法做重试与死信重放。
--
-- 幂等靠唯一键，不靠应用判断：
--   (endpoint_id, biz_type, biz_key) 唯一 → 同一业务对象重复提交只会命中同一条任务，
--   并发下由数据库兜底（应用层先查再插会被并发穿透）。
--
-- 状态语义：
--   PREVIEW  只生成报文快照（dry-run），永不发送；
--   PENDING  待发送（含定时重试扫描到的）；
--   RUNNING  发送中（异常中断的由定时任务按超时回收）；
--   SUCCESS  对端 2xx；
--   FAILED   本次失败但还有重试次数，等 next_retry_at；
--   DEAD     重试用尽 → 死信，只能人工重放。
-- =====================================================================

CREATE TABLE itg_endpoint (
    id          bigint       PRIMARY KEY,
    company_id  bigint       NOT NULL,
    code        varchar(64)  NOT NULL,
    name        varchar(200) NOT NULL,
    system_code varchar(32)  NOT NULL DEFAULT 'OTHER',   -- KINGDEE/WMS/MES/OTHER
    base_url    varchar(500) NOT NULL,
    auth_type   varchar(16)  NOT NULL DEFAULT 'NONE',    -- NONE/BASIC/BEARER/HEADER
    auth_config jsonb        NOT NULL DEFAULT '{}'::jsonb, -- 敏感，接口不回传
    headers     jsonb        NOT NULL DEFAULT '{}'::jsonb,
    timeout_ms  int          NOT NULL DEFAULT 10000,
    enabled     boolean      NOT NULL DEFAULT true,
    remark      varchar(1000),
    created_by  bigint,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_by  bigint,
    updated_at  timestamptz  NOT NULL DEFAULT now(),
    deleted     boolean      NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_itg_endpoint_code
    ON itg_endpoint (company_id, code) WHERE deleted = false;

CREATE TABLE itg_mapping (
    id              bigint      PRIMARY KEY,
    company_id      bigint      NOT NULL,
    endpoint_id     bigint      NOT NULL,
    object_code     varchar(64) NOT NULL,                -- Material/Bom/Doc/Route/Requisition
    direction       varchar(8)  NOT NULL DEFAULT 'OUT',  -- OUT/IN
    payload_version varchar(16) NOT NULL DEFAULT '1.0',
    rules           jsonb       NOT NULL DEFAULT '[]'::jsonb, -- [{source,target,transform,required}]
    enabled         boolean     NOT NULL DEFAULT true,
    remark          varchar(1000),
    created_by      bigint,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_by      bigint,
    updated_at      timestamptz NOT NULL DEFAULT now(),
    deleted         boolean     NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_itg_mapping
    ON itg_mapping (endpoint_id, object_code, direction) WHERE deleted = false;

CREATE TABLE itg_job (
    id              bigint       PRIMARY KEY,
    company_id      bigint       NOT NULL,
    job_no          varchar(64)  NOT NULL,                -- ITG-000001
    endpoint_id     bigint       NOT NULL,
    mapping_id      bigint,
    biz_type        varchar(64)  NOT NULL,                -- 等于 object_code
    biz_key         varchar(200) NOT NULL,                -- 幂等键：业务对象标识
    direction       varchar(8)   NOT NULL DEFAULT 'OUT',
    status          varchar(16)  NOT NULL DEFAULT 'PENDING',
    attempt         int          NOT NULL DEFAULT 0,
    max_attempt     int          NOT NULL DEFAULT 3,
    next_retry_at   timestamptz,
    request_payload jsonb        NOT NULL DEFAULT '{}'::jsonb, -- 发送前固定，重放用快照
    response_body   text,
    http_status     int,
    error_msg       varchar(1000),
    duration_ms     int,
    started_at      timestamptz,
    finished_at     timestamptz,
    created_by      bigint,
    created_at      timestamptz  NOT NULL DEFAULT now(),
    updated_at      timestamptz  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_itg_job_biz ON itg_job (endpoint_id, biz_type, biz_key);
CREATE INDEX idx_itg_job_retry ON itg_job (status, next_retry_at);
CREATE INDEX idx_itg_job_list ON itg_job (company_id, created_at DESC);

CREATE TABLE itg_log (
    id               bigint       PRIMARY KEY,
    company_id       bigint       NOT NULL,
    job_id           bigint       NOT NULL,
    attempt          int          NOT NULL,
    status           varchar(16)  NOT NULL,               -- SUCCESS/FAILED
    http_status      int,
    duration_ms      int,
    request_snippet  varchar(2000),
    response_snippet varchar(2000),
    error_msg        varchar(1000),
    created_at       timestamptz  NOT NULL DEFAULT now()
);
CREATE INDEX idx_itg_log_job ON itg_log (job_id, attempt);

INSERT INTO sys_permission (code, module, action, name, name_en, perm_type) VALUES
    ('itg.config.read',  'ITG', 'READ',  '集成配置查看', 'Integration config read',  'FUNCTION'),
    ('itg.config.write', 'ITG', 'WRITE', '集成配置维护', 'Integration config write', 'FUNCTION'),
    ('itg.job.read',     'ITG', 'READ',  '集成任务查看', 'Integration job read',     'FUNCTION'),
    ('itg.job.write',    'ITG', 'WRITE', '集成任务执行', 'Integration job write',    'FUNCTION')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission (role_id, permission_code)
SELECT r.id, p.code
FROM sys_role r
JOIN sys_permission p ON p.code IN
    ('itg.config.read', 'itg.config.write', 'itg.job.read', 'itg.job.write')
WHERE r.code IN ('SYS_ADMIN', 'BUSINESS')
ON CONFLICT DO NOTHING;
