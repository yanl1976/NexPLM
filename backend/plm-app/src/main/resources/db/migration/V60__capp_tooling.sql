-- =====================================================================
-- 批次 C-3｜工装管理
--
-- 旧系统取证（PLMSOFT/Server/InforCenter/CAPP/Tooling）：
--   ToolingListView / ToolingApplyListView / ToolingApplyBillPage（工装申请单）
--   CreateToolingAfterToolingApplyPublishedPage（申请单发布后生成工装）
--   ToolingImpactAnalysis（工装被工序引用后的影响分析）
--   WorlflowPlaceOnResourcePage（把工装放到资源/工序上）
--   → 即「台账 + 申请单（审批后建台账）+ 借用/占用（挂工艺资源）」三段。
-- =====================================================================

CREATE TABLE capp_tooling (
    id           bigint       PRIMARY KEY,
    company_id   bigint       NOT NULL,
    code         varchar(64)  NOT NULL,
    name         varchar(200) NOT NULL,
    -- FIXTURE=夹具 / GAUGE=量具 / MOLD=模具 / CUTTER=刀具 / AID=辅具
    tooling_type varchar(16)  NOT NULL DEFAULT 'FIXTURE',
    spec         varchar(200),
    -- IDLE=在库 / IN_USE=使用中 / MAINTAIN=维修 / SCRAP=报废
    status       varchar(16)  NOT NULL DEFAULT 'IDLE',
    location     varchar(100),
    owner_id     bigint,
    purchase_date date,
    remark       varchar(500),
    deleted      boolean      NOT NULL DEFAULT false,
    created_by   bigint,
    created_at   timestamptz  NOT NULL DEFAULT now(),
    updated_at   timestamptz  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uk_capp_tooling_code ON capp_tooling (company_id, code) WHERE deleted = false;

CREATE TABLE capp_tooling_apply (
    id           bigint       PRIMARY KEY,
    company_id   bigint       NOT NULL,
    code         varchar(64)  NOT NULL,
    tooling_type varchar(16)  NOT NULL DEFAULT 'FIXTURE',
    tooling_name varchar(200) NOT NULL,
    spec         varchar(200),
    reason       varchar(500),
    -- DRAFT=草稿 / SUBMITTED=已提交 / APPROVED=已批准 / REJECTED=已驳回
    status       varchar(16)  NOT NULL DEFAULT 'DRAFT',
    applicant_id bigint,
    approved_by  bigint,
    approved_at  timestamptz,
    -- 批准后生成的工装台账 id（旧系统 CreateToolingAfterToolingApplyPublished）
    tooling_id   bigint,
    remark       varchar(500),
    deleted      boolean      NOT NULL DEFAULT false,
    created_at   timestamptz  NOT NULL DEFAULT now(),
    updated_at   timestamptz  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uk_capp_tooling_apply_code ON capp_tooling_apply (company_id, code) WHERE deleted = false;

CREATE TABLE capp_tooling_borrow (
    id          bigint       PRIMARY KEY,
    company_id  bigint       NOT NULL,
    tooling_id  bigint       NOT NULL,
    borrower_id bigint,
    -- 占用在哪条工艺路线/资源上（旧 WorlflowPlaceOnResource）
    routing_id  bigint,
    borrowed_at timestamptz  NOT NULL DEFAULT now(),
    returned_at timestamptz,
    remark      varchar(500)
);

CREATE INDEX idx_capp_tooling_borrow ON capp_tooling_borrow (tooling_id, returned_at);

INSERT INTO code_rule (id, code, name, object_type, segments_json, reset_policy, status, remark) VALUES
    (910014, 'CappTooling', '工装', 'CappTooling',
     '[{"type":"TEXT","value":"TL-"},{"type":"SEQ","width":6,"radix":"DECIMAL"}]', 'NEVER', 'ACTIVE', '批次 C-3 新增'),
    (910015, 'CappToolingApply', '工装申请单', 'CappToolingApply',
     '[{"type":"TEXT","value":"TA-"},{"type":"SEQ","width":6,"radix":"DECIMAL"}]', 'NEVER', 'ACTIVE', '批次 C-3 新增')
ON CONFLICT DO NOTHING;
