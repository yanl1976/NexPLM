-- =====================================================================
-- P7-1 项目与 EPS：项目主档 + EPS 目录结构 + 项目成员
--
-- EPS（Enterprise Project Structure）：项目的目录树，用于「按部门/产品线归类项目」。
--   path 存物化路径（/1/5/），支持「整棵子树」一条 LIKE 查询，避免递归 CTE。
--
-- 项目状态机（与 CAPP 一样先用简单四态，不接 meta_lifecycle）：
--   DRAFT（立项草稿，可改可删）→ ACTIVE（执行中，可暂停）→ PAUSED（暂停）→ CLOSED（已结项）
--   注意：ACTIVE/PAUSED 之间可互转；CLOSED 为终态（不可再改，只能查看）。
--
-- 进度 progress：本阶段先人工维护（0~100），P7-2 任务上线后由任务完成度自动汇总覆盖。
-- =====================================================================

CREATE TABLE prj_eps (
    id            bigint       PRIMARY KEY,
    company_id    bigint       NOT NULL,
    parent_id     bigint,                                     -- 空 = 根节点
    code          varchar(64)  NOT NULL,
    name          varchar(200) NOT NULL,
    path          varchar(1000) NOT NULL DEFAULT '/',         -- 物化路径 /父id/本id/
    sort_no       int          NOT NULL DEFAULT 10,
    remark        varchar(500),
    created_by    bigint,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    updated_by    bigint,
    updated_at    timestamptz  NOT NULL DEFAULT now(),
    deleted       boolean      NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_prj_eps_code ON prj_eps (company_id, code) WHERE deleted = false;
CREATE INDEX idx_prj_eps_tree ON prj_eps (company_id, parent_id, sort_no) WHERE deleted = false;
CREATE INDEX idx_prj_eps_path ON prj_eps (company_id, path) WHERE deleted = false;

CREATE TABLE prj_project (
    id            bigint       PRIMARY KEY,
    company_id    bigint       NOT NULL,
    code          varchar(64)  NOT NULL,                      -- PJ-000001
    name          varchar(200) NOT NULL,
    eps_id        bigint,                                     -- 所属 EPS 节点（可空=未归类）
    manager_id    bigint,                                     -- 项目经理（sys_user.id）
    type          varchar(32)  NOT NULL DEFAULT 'RD',         -- RD 研发 / ORDER 订单 / OTHER 其它
    status        varchar(16)  NOT NULL DEFAULT 'DRAFT',       -- DRAFT / ACTIVE / PAUSED / CLOSED
    priority      int          NOT NULL DEFAULT 3,             -- 1 最高 ~ 5 最低
    plan_start    date,
    plan_end      date,
    actual_start  date,
    actual_end    date,
    progress      int          NOT NULL DEFAULT 0,             -- 0~100
    remark        varchar(500),
    created_by    bigint,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    updated_by    bigint,
    updated_at    timestamptz  NOT NULL DEFAULT now(),
    deleted       boolean      NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_prj_project_code ON prj_project (company_id, code) WHERE deleted = false;
CREATE INDEX idx_prj_project_list ON prj_project (company_id, status, updated_at DESC) WHERE deleted = false;
CREATE INDEX idx_prj_project_eps ON prj_project (company_id, eps_id, status) WHERE deleted = false;
CREATE INDEX idx_prj_project_manager ON prj_project (company_id, manager_id) WHERE deleted = false;

CREATE TABLE prj_member (
    id            bigint       PRIMARY KEY,
    company_id    bigint       NOT NULL,
    project_id    bigint       NOT NULL,
    user_id       bigint       NOT NULL,
    role          varchar(32)  NOT NULL DEFAULT 'MEMBER',      -- MANAGER / MEMBER / VIEWER
    created_at    timestamptz  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_prj_member ON prj_member (project_id, user_id);
CREATE INDEX idx_prj_member_user ON prj_member (company_id, user_id);

-- ---------------------------------------------------------------------
-- 权限点
-- ---------------------------------------------------------------------
INSERT INTO sys_permission (code, module, action, name, name_en, perm_type) VALUES
    ('prj.project.read',  'PRJ', 'READ',  '项目查看', 'Project read',  'FUNCTION'),
    ('prj.project.write', 'PRJ', 'WRITE', '项目维护', 'Project write', 'FUNCTION'),
    ('prj.eps.write',     'PRJ', 'WRITE', 'EPS 结构维护', 'EPS write', 'FUNCTION')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission (role_id, permission_code)
SELECT r.id, p.code
  FROM sys_role r
  JOIN sys_permission p ON p.code LIKE 'prj.%'
 WHERE r.code IN ('SYS_ADMIN', 'BUSINESS')
ON CONFLICT DO NOTHING;
