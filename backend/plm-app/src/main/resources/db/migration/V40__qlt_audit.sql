-- =====================================================================
-- P8-3-2 IATF16949 体系审核记录：审核计划 + 检查表 + 不符合项 → 转质量问题
--
-- 三段式：
--   qlt_audit_plan     一次审核（内审/外审/供方审核/过程审核），有计划日与实际日
--   qlt_audit_item     检查表条目（条款 + 提问 + 结果 OK/NG/NA），整表替换保存
--   qlt_audit_finding  不符合项（NG 的正式化），**转成 qlt_issue 后走 P8-1 的
--                      8D 整改闭环**——审核模块自己不重建一套整改流程。
--
-- 关闭口径（刻意的强约束）：
--   * 检查表为空不能开始审核（没有检查表就不是审核）；
--   * 存在未关闭的不符合项不能关闭审核（否则发现项会被"结项"掩盖）；
--   * 不符合项必须先转质量问题、且该问题已关闭，才允许关闭（闭环才收口）。
-- =====================================================================

CREATE TABLE qlt_audit_plan (
    id          bigint       PRIMARY KEY,
    company_id  bigint       NOT NULL,
    code        varchar(64)  NOT NULL,                      -- AU-000001
    title       varchar(200) NOT NULL,
    audit_type  varchar(16)  NOT NULL DEFAULT 'INTERNAL',
                                                            -- INTERNAL/EXTERNAL/SUPPLIER/PROCESS
    standard    varchar(200),                               -- 依据标准（IATF16949:2016 / 内部程序文件）
    scope       varchar(500),                               -- 审核范围（部门/过程/产品）
    planned_date date,
    actual_date  date,
    auditor_id  bigint,                                     -- 主审
    auditee     varchar(200),                               -- 被审部门/过程负责人
    status      varchar(16)  NOT NULL DEFAULT 'PLANNED',    -- PLANNED/IN_PROGRESS/CLOSED
    summary     varchar(2000),
    remark      varchar(1000),
    created_by  bigint,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_by  bigint,
    updated_at  timestamptz  NOT NULL DEFAULT now(),
    deleted     boolean      NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_qlt_audit_plan_code ON qlt_audit_plan (company_id, code) WHERE deleted = false;
CREATE INDEX idx_qlt_audit_plan_status ON qlt_audit_plan (company_id, status, planned_date)
    WHERE deleted = false;

CREATE TABLE qlt_audit_item (
    id         bigint       PRIMARY KEY,
    company_id bigint       NOT NULL,
    plan_id    bigint       NOT NULL,
    item_no    varchar(32)  NOT NULL,                       -- 1 / 4.4.1-1
    clause     varchar(64),                                 -- 标准条款号
    question   varchar(500) NOT NULL,                       -- 检查什么
    result     varchar(16)  NOT NULL DEFAULT 'PENDING',     -- PENDING/OK/NG/NA
    evidence   varchar(1000),                               -- 抽样证据/记录编号
    sort_no    int          NOT NULL DEFAULT 10,
    deleted    boolean      NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_qlt_audit_item ON qlt_audit_item (plan_id, item_no) WHERE deleted = false;

CREATE TABLE qlt_audit_finding (
    id          bigint       PRIMARY KEY,
    company_id  bigint       NOT NULL,
    plan_id     bigint       NOT NULL,
    item_id     bigint,                                     -- 由哪条检查项发现（可空）
    code        varchar(64)  NOT NULL,                      -- FD-000001
    title       varchar(200) NOT NULL,
    description varchar(2000),
    severity    varchar(16)  NOT NULL DEFAULT 'MINOR',      -- MINOR/MAJOR/CRITICAL
    due_date    date,
    status      varchar(16)  NOT NULL DEFAULT 'OPEN',       -- OPEN/CLOSED
    issue_id    bigint,                                     -- 转成的质量问题（qlt_issue.id）
    issue_code  varchar(64),
    closed_at   timestamptz,
    created_by  bigint,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_by  bigint,
    updated_at  timestamptz  NOT NULL DEFAULT now(),
    deleted     boolean      NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_qlt_audit_finding_code ON qlt_audit_finding (company_id, code)
    WHERE deleted = false;
CREATE INDEX idx_qlt_audit_finding_plan ON qlt_audit_finding (plan_id, status) WHERE deleted = false;

INSERT INTO sys_permission (code, module, action, name, name_en, perm_type) VALUES
    ('qlt.audit.read',  'QLT', 'READ',  '体系审核查看', 'Quality audit read',  'FUNCTION'),
    ('qlt.audit.write', 'QLT', 'WRITE', '体系审核维护', 'Quality audit write', 'FUNCTION')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission (role_id, permission_code)
SELECT r.id, p.code
FROM sys_role r
JOIN sys_permission p ON p.code = 'qlt.audit.read' OR p.code = 'qlt.audit.write'
WHERE r.code IN ('SYS_ADMIN', 'BUSINESS')
ON CONFLICT DO NOTHING;
