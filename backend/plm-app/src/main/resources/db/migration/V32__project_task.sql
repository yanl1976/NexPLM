-- =====================================================================
-- P7-2 WBS 任务 + 任务依赖（甘特的数据底座）
--
-- 层级：用 parent_id 自关联表达 WBS，wbs_code 存「1 / 1.2 / 1.2.3」这样的层级编码，
--       既能当显示序号，也能直接字符串排序（前缀天然有序），不必递归查询。
--
-- 进度汇总（P7-2 的核心目标）：项目 progress 不再人工维护，
--       每次任务变更后由「叶子任务进度 × 工期权重」加权汇总写回 prj_project.progress。
--       只算叶子——父任务若也填进度会与子任务重复计数。
--
-- 依赖：prj_task_dep 存四类依赖（FS/SS/FF/SF）+ 滞后天数，服务端建依赖时做环检测。
--       甘特图只读这张表画箭头，不反向推算计划日期（排程引擎 P7-3 再做）。
-- =====================================================================

CREATE TABLE prj_task (
    id            bigint       PRIMARY KEY,
    company_id    bigint       NOT NULL,
    project_id    bigint       NOT NULL,
    parent_id     bigint,                                   -- 空 = 一级任务
    wbs_code      varchar(64)  NOT NULL,                    -- 1 / 1.2 / 1.2.3
    name          varchar(200) NOT NULL,
    type          varchar(16)  NOT NULL DEFAULT 'TASK',     -- TASK 任务 / MILESTONE 里程碑
    status        varchar(16)  NOT NULL DEFAULT 'NOT_STARTED',
                                                            -- NOT_STARTED / IN_PROGRESS / DONE / BLOCKED
    sort_no       int          NOT NULL DEFAULT 10,
    plan_start    date,
    plan_end      date,
    actual_start  date,
    actual_end    date,
    duration_days int,                                      -- 为空时按 plan_start~plan_end 推算
    progress      int          NOT NULL DEFAULT 0,          -- 0~100
    owner_id      bigint,
    remark        varchar(500),
    created_by    bigint,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    updated_by    bigint,
    updated_at    timestamptz  NOT NULL DEFAULT now(),
    deleted       boolean      NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_prj_task_wbs ON prj_task (project_id, wbs_code) WHERE deleted = false;
CREATE INDEX idx_prj_task_tree ON prj_task (project_id, parent_id, sort_no) WHERE deleted = false;
CREATE INDEX idx_prj_task_owner ON prj_task (company_id, owner_id) WHERE deleted = false;

CREATE TABLE prj_task_dep (
    id            bigint       PRIMARY KEY,
    company_id    bigint       NOT NULL,
    project_id    bigint       NOT NULL,
    pred_task_id  bigint       NOT NULL,                    -- 前置任务
    succ_task_id  bigint       NOT NULL,                    -- 后续任务
    dep_type      varchar(2)   NOT NULL DEFAULT 'FS',       -- FS / SS / FF / SF
    lag_days      int          NOT NULL DEFAULT 0,          -- 正=滞后，负=提前
    created_at    timestamptz  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_prj_task_dep ON prj_task_dep (pred_task_id, succ_task_id);
CREATE INDEX idx_prj_task_dep_succ ON prj_task_dep (succ_task_id);
CREATE INDEX idx_prj_task_dep_project ON prj_task_dep (project_id);
