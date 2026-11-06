-- =====================================================================
-- P4 工作流：委托规则
--
-- 说明：
--   1. 委托是「规则」而非一次性转办：owner 在 [start_at, end_at] 区间内、
--      且流程 key 匹配（为空表示全部流程）时，待办对方可见，对方可代为办理；
--   2. 任务的 assignee 不变，代办只在查询与权限层生效，避免改动引擎数据后无法回退；
--   3. 加签不需要业务表（走引擎多实例运行时 API），故本迁移只有委托。
-- =====================================================================

CREATE TABLE wf_delegate (
    id          bigint       PRIMARY KEY,
    owner_id    bigint       NOT NULL,
    delegate_id bigint       NOT NULL,
    start_at    timestamptz  NOT NULL,
    end_at      timestamptz  NOT NULL,
    process_key varchar(128),
    enabled     boolean      NOT NULL DEFAULT true,
    remark      varchar(500),
    created_by  bigint,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_at  timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT ck_wf_delegate_range CHECK (end_at > start_at),
    CONSTRAINT ck_wf_delegate_self CHECK (owner_id <> delegate_id)
);
CREATE UNIQUE INDEX uk_wf_delegate ON wf_delegate (owner_id, delegate_id, start_at);
CREATE INDEX idx_wf_delegate_delegate ON wf_delegate (delegate_id, enabled, start_at, end_at);
CREATE INDEX idx_wf_delegate_owner ON wf_delegate (owner_id, enabled);
