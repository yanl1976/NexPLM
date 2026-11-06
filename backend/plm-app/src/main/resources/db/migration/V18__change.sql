-- =====================================================================
-- P5 变更管理：变更申请（ECR）/ 变更通知（ECN）/ 变更问题（Problem）/ 关联对象
--
-- 关系：ECR 1—N ECN 1—N Problem；ECR 1—N 关联对象（受影响/影响/参考）
-- 阶段状态机（先用常量校验，后续接 meta_state 元数据定义）：
--   ECR:     DRAFT → SUBMITTED → REVIEWING → APPROVED → NOTICED → EXECUTING → CLOSED
--            分支：REJECTED（评审不通过）/ CANCELLED（撤回）
--   ECN:     DRAFT → ISSUED → EXECUTING → CLOSED
--   Problem: OPEN → PROCESSING → CLOSED
-- 版本执行：change 执行时对 AFFECTED 对象「升版」——新增一行 plm_object（revision+1、iteration=1、
--           is_latest=true），旧行 is_latest 置 false，master_id 不变（对象溯源）。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 变更申请 ECR
-- ---------------------------------------------------------------------
CREATE TABLE chg_ecr (
    id           bigint       PRIMARY KEY,
    company_id   bigint       NOT NULL,
    code         varchar(64)  NOT NULL,
    title        varchar(200) NOT NULL,
    change_type  varchar(32)  NOT NULL,                      -- DESIGN / PROCESS
    category     varchar(64),                                -- 分类（可由元数据约束）
    reason       varchar(2000),
    feasibility  varchar(1000),                              -- 可行性分析
    priority     varchar(16)  NOT NULL DEFAULT 'NORMAL',     -- HIGH / NORMAL / LOW
    status       varchar(32)  NOT NULL DEFAULT 'DRAFT',
    requester_id bigint       NOT NULL,
    receiver_id  bigint,                                     -- 接收人（默认变更专员）
    project_no   varchar(64),                                -- 关联项目号（可选）
    target_date  date,                                       -- 期望完成日期
    approved_at  timestamptz,
    closed_at    timestamptz,
    remark       varchar(500),
    created_by   bigint,
    created_at   timestamptz  NOT NULL DEFAULT now(),
    updated_by   bigint,
    updated_at   timestamptz  NOT NULL DEFAULT now(),
    deleted      boolean      NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_chg_ecr_code ON chg_ecr (company_id, code) WHERE deleted = false;
CREATE INDEX idx_chg_ecr_list ON chg_ecr (company_id, status, created_at DESC) WHERE deleted = false;
CREATE INDEX idx_chg_ecr_requester ON chg_ecr (requester_id);

-- ---------------------------------------------------------------------
-- 变更通知 ECN
-- ---------------------------------------------------------------------
CREATE TABLE chg_ecn (
    id          bigint       PRIMARY KEY,
    company_id  bigint       NOT NULL,
    code        varchar(64)  NOT NULL,
    ecr_id      bigint       NOT NULL,
    title       varchar(200) NOT NULL,
    status      varchar(32)  NOT NULL DEFAULT 'DRAFT',
    executor_id bigint,                                      -- 执行人
    plan_date   date,
    issued_at   timestamptz,
    closed_at   timestamptz,
    remark      varchar(500),
    created_by  bigint,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_at  timestamptz  NOT NULL DEFAULT now(),
    deleted     boolean      NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_chg_ecn_code ON chg_ecn (company_id, code) WHERE deleted = false;
CREATE INDEX idx_chg_ecn_ecr ON chg_ecn (ecr_id);

-- ---------------------------------------------------------------------
-- 变更问题 Problem（ECN 执行中发现的问题，闭环后才允许关闭 ECN）
-- ---------------------------------------------------------------------
CREATE TABLE chg_problem (
    id          bigint        PRIMARY KEY,
    company_id  bigint        NOT NULL,
    code        varchar(64)   NOT NULL,
    ecn_id      bigint        NOT NULL,
    description varchar(2000) NOT NULL,
    status      varchar(32)   NOT NULL DEFAULT 'OPEN',
    owner_id    bigint,
    due_date    date,
    closed_at   timestamptz,
    created_by  bigint,
    created_at  timestamptz   NOT NULL DEFAULT now(),
    updated_at  timestamptz   NOT NULL DEFAULT now(),
    deleted     boolean       NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_chg_problem_code ON chg_problem (company_id, code) WHERE deleted = false;
CREATE INDEX idx_chg_problem_ecn ON chg_problem (ecn_id);

-- ---------------------------------------------------------------------
-- 变更关联对象：受影响（要改）/ 影响（被波及需评估）/ 参考（仅提供上下文）
-- action 为对受影响对象的操作意图：REVISE 升版 / OBSOLETE 废弃 / RELEASE 发布
-- ---------------------------------------------------------------------
CREATE TABLE chg_object (
    id            bigint       PRIMARY KEY,
    company_id    bigint       NOT NULL,
    ecr_id        bigint       NOT NULL,
    object_type   varchar(64)  NOT NULL,
    object_id     bigint       NOT NULL,
    relation      varchar(16)  NOT NULL DEFAULT 'AFFECTED',
    action        varchar(16),                               -- AFFECTED 必填，其它可空
    revision_from int,
    revision_to   int,
    exec_status   varchar(16)  NOT NULL DEFAULT 'PENDING',   -- PENDING / DONE / SKIPPED
    remark        varchar(500),
    created_by    bigint,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    updated_at    timestamptz  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_chg_object ON chg_object (ecr_id, object_type, object_id, relation);
CREATE INDEX idx_chg_object_ref ON chg_object (object_type, object_id);
CREATE INDEX idx_chg_object_ecr ON chg_object (ecr_id, relation);

-- ---------------------------------------------------------------------
-- 权限点
-- ---------------------------------------------------------------------
INSERT INTO sys_permission (code, module, action, name, name_en, perm_type) VALUES
    ('chg.ecr.read',  'CHG', 'READ',  '变更管理查看', 'Change read',  'FUNCTION'),
    ('chg.ecr.write', 'CHG', 'WRITE', '变更管理维护', 'Change write', 'FUNCTION')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission (role_id, permission_code)
SELECT r.id, p.code
  FROM sys_role r
  JOIN sys_permission p ON p.code LIKE 'chg.%'
 WHERE r.code IN ('SYS_ADMIN', 'BUSINESS')
ON CONFLICT DO NOTHING;
