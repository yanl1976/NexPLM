-- =====================================================================
-- P6-9 与 ERP 的工艺下发接口【预留】
--
-- 计划里明确「与 ERP 的工艺下发接口预留（P9 实现）」，因此本阶段只做 PLM 侧的三件事：
--   1) 把「要下发什么」固化成一份**载荷快照**（路线 + 工序 + 工步 + 资源 + 工时）；
--   2) 记录每次下发尝试（状态 + 回执消息），便于后续排查与重放；
--   3) 提供读取端点，让 P9 的适配器（HTTP / 中间表 / 文件）从这个端点取数。
--
-- 明确不做（P9）：
--   - 真实 ERP 契约（字段映射、认证、幂等键）；
--   - 自动重试与回执对账；
--   - 下发范围控制（哪些路线允许下发、变更后是否需要重新下发）。
--
-- 载荷存 jsonb 而不是每次实时拼装：下发是「某一时刻的口径」，事后路线被改（甚至归档）时，
-- 仍应能回答「当初到底发了什么」，这是审计与责任追溯的底线。
-- =====================================================================

CREATE TABLE capp_erp_dispatch (
    id           bigint       PRIMARY KEY,
    company_id   bigint       NOT NULL,
    code         varchar(64)  NOT NULL,                   -- ED-000001
    routing_id   bigint       NOT NULL,
    object_id    bigint       NOT NULL,
    status       varchar(16)  NOT NULL DEFAULT 'PENDING', -- PENDING 待发送 / SENT 已发送 / FAILED 失败 / VOID 作废
    payload_json jsonb        NOT NULL,                   -- 下发载荷快照（不可变）
    payload_sha  varchar(64),                             -- 载荷 sha256，供 ERP 侧做幂等/去重
    message      varchar(500),                            -- 回执/错误信息
    created_by   bigint,
    created_at   timestamptz  NOT NULL DEFAULT now(),
    updated_by   bigint,
    updated_at   timestamptz  NOT NULL DEFAULT now(),
    deleted      boolean      NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_capp_erp_dispatch_code
    ON capp_erp_dispatch (company_id, code) WHERE deleted = false;
CREATE INDEX idx_capp_erp_dispatch_routing
    ON capp_erp_dispatch (company_id, routing_id, created_at DESC) WHERE deleted = false;
