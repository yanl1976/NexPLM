-- =====================================================================
-- P8-1 质量问题闭环：登记 → 分派 → 多轮整改 → 验证关闭
--
-- 状态机（每个动作一个接口，保存接口不暴露 status，避免前端绕过校验）：
--   OPEN ──assign──▶ ASSIGNED ──submit(措施全完成)──▶ VERIFYING
--     ▲                                               │
--     │                                    verify FAIL │ verify PASS
--     └────────────── IN_PROGRESS ◀──(round+1)────────┤
--                                                     ▼
--                                                  CLOSED
--
-- 轮次（round_no）是这套模型的关键：验证不通过不是"打回同一条措施"，而是
-- 开启新一轮整改——第 1 轮的 8D 分析与第 2 轮的补充分析要能分开展示与追责，
-- 所以措施表必须带 round_no，打回时 issue.round_no +1，旧轮次原样保留。
--
-- 措施类型按 8D 骨架固定枚举（CONTAINMENT/D1..D8 的常用归类）：
--   CONTAINMENT 临时遏制 / ROOT_CAUSE 根因分析 / CORRECTIVE 纠正措施 /
--   PREVENTIVE 预防措施 / VERIFY 验证记录
-- 全流程不建"8D 模板表"：模板只是提示性骨架，P8-2 再做可配模板。
-- =====================================================================

CREATE TABLE qlt_issue (
    id          bigint       PRIMARY KEY,
    company_id  bigint       NOT NULL,
    code        varchar(64)  NOT NULL,                      -- QI-000001
    title       varchar(200) NOT NULL,
    description varchar(2000),
    source      varchar(16)  NOT NULL DEFAULT 'INTERNAL',   -- INTERNAL/CUSTOMER/SUPPLIER/AUDIT
    severity    varchar(16)  NOT NULL DEFAULT 'MAJOR',      -- MINOR/MAJOR/CRITICAL
    status      varchar(16)  NOT NULL DEFAULT 'OPEN',
                                                            -- OPEN/ASSIGNED/IN_PROGRESS/VERIFYING/CLOSED
    round_no    int          NOT NULL DEFAULT 1,            -- 当前整改轮次
    object_type varchar(64),                                -- 关联对象（ItemMaster/DocMaster…，可空）
    object_id   bigint,
    object_code varchar(128),                               -- 冗余展示用（对象删了也还能看出问题出在哪个物料）
    project_id  bigint,
    supplier_id bigint,
    owner_id    bigint,                                     -- 责任人
    reported_by bigint,
    reported_at timestamptz  NOT NULL DEFAULT now(),
    due_date    date,
    closed_at   timestamptz,
    verify_by   bigint,
    verify_at   timestamptz,
    verify_note varchar(1000),
    remark      varchar(1000),
    created_by  bigint,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_by  bigint,
    updated_at  timestamptz  NOT NULL DEFAULT now(),
    deleted     boolean      NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_qlt_issue_code ON qlt_issue (company_id, code) WHERE deleted = false;
CREATE INDEX idx_qlt_issue_status ON qlt_issue (company_id, status, due_date) WHERE deleted = false;
CREATE INDEX idx_qlt_issue_owner ON qlt_issue (company_id, owner_id, status) WHERE deleted = false;
CREATE INDEX idx_qlt_issue_object ON qlt_issue (object_type, object_id) WHERE deleted = false;

CREATE TABLE qlt_issue_action (
    id          bigint       PRIMARY KEY,
    company_id  bigint       NOT NULL,
    issue_id    bigint       NOT NULL,
    round_no    int          NOT NULL DEFAULT 1,            -- 属于第几轮整改
    action_type varchar(16)  NOT NULL DEFAULT 'CORRECTIVE',
                                                            -- CONTAINMENT/ROOT_CAUSE/CORRECTIVE/PREVENTIVE/VERIFY
    content     varchar(2000) NOT NULL,
    owner_id    bigint,
    due_date    date,
    status      varchar(16)  NOT NULL DEFAULT 'PENDING',    -- PENDING/DONE
    done_at     timestamptz,
    created_by  bigint,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_by  bigint,
    updated_at  timestamptz  NOT NULL DEFAULT now(),
    deleted     boolean      NOT NULL DEFAULT false
);
CREATE INDEX idx_qlt_action_issue ON qlt_issue_action (issue_id, round_no, created_at)
    WHERE deleted = false;
CREATE INDEX idx_qlt_action_owner ON qlt_issue_action (company_id, owner_id, status)
    WHERE deleted = false;

INSERT INTO sys_permission (code, module, action, name, name_en, perm_type) VALUES
    ('qlt.issue.read',  'QLT', 'READ',  '质量问题查看', 'Quality issue read',  'FUNCTION'),
    ('qlt.issue.write', 'QLT', 'WRITE', '质量问题维护', 'Quality issue write', 'FUNCTION')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission (role_id, permission_code)
SELECT r.id, p.code
FROM sys_role r
JOIN sys_permission p ON p.code LIKE 'qlt.%'
WHERE r.code IN ('SYS_ADMIN', 'BUSINESS')
ON CONFLICT DO NOTHING;
