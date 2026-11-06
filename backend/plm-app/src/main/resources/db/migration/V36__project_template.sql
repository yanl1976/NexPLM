-- =====================================================================
-- P7-6 项目模板：立项时「一键长出一个项目」
--
-- 模板 = 项目类型 + WBS 任务骨架 + 文档目录骨架。三类数据都挂在模板下：
--   prj_template_task   任务骨架：用「相对项目开始日的偏移 + 工期」记，
--                       立项时按实际开工日推算计划起止——模板里存绝对日期毫无意义。
--   prj_template_folder 目录骨架：只在模板里存「逻辑路径」，立项时才生成物化路径
--                       （物化路径依赖新 id，模板阶段还没有）。
--
-- WBS 编码沿用模板里维护的 code（1 / 1.2 / 1.2.3），立项时原样复制；
-- 同一模板内唯一即可，生成到单个项目里天然不冲突。
--
-- 模板只做骨架，不做「模板版本化」：模板改动不该回溯已立项项目（项目生成时就已落地成实体）。
-- =====================================================================

CREATE TABLE prj_template (
    id          bigint       PRIMARY KEY,
    company_id  bigint       NOT NULL,
    code        varchar(64)  NOT NULL,                      -- PT-000001
    name        varchar(200) NOT NULL,
    type        varchar(32)  NOT NULL DEFAULT 'RD',         -- RD / ORDER / OTHER
    remark      varchar(500),
    enabled     boolean      NOT NULL DEFAULT true,
    created_by  bigint,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_by  bigint,
    updated_at  timestamptz  NOT NULL DEFAULT now(),
    deleted     boolean      NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_prj_template_code ON prj_template (company_id, code) WHERE deleted = false;
CREATE INDEX idx_prj_template_list ON prj_template (company_id, enabled, updated_at DESC)
    WHERE deleted = false;

CREATE TABLE prj_template_task (
    id            bigint       PRIMARY KEY,
    company_id    bigint       NOT NULL,
    template_id   bigint       NOT NULL,
    parent_id     bigint,                                   -- 模板内父任务（立项时映射为新 id）
    wbs_code      varchar(64)  NOT NULL,                    -- 1 / 1.2 / 1.2.3
    name          varchar(200) NOT NULL,
    type          varchar(16)  NOT NULL DEFAULT 'TASK',     -- TASK / MILESTONE
    start_offset  int          NOT NULL DEFAULT 0,          -- 相对项目开工日的第几天
    duration_days int          NOT NULL DEFAULT 1,          -- 里程碑填 0 也按 1 天落库
    sort_no       int          NOT NULL DEFAULT 10,
    remark        varchar(500),
    deleted       boolean      NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_prj_template_task_wbs
    ON prj_template_task (template_id, wbs_code) WHERE deleted = false;
CREATE INDEX idx_prj_template_task_tree
    ON prj_template_task (template_id, parent_id, sort_no) WHERE deleted = false;

CREATE TABLE prj_template_folder (
    id          bigint       PRIMARY KEY,
    company_id  bigint       NOT NULL,
    template_id bigint       NOT NULL,
    parent_path varchar(500) NOT NULL DEFAULT '/',          -- 父「逻辑路径」，根为 /
    name        varchar(200) NOT NULL,
    sort_no     int          NOT NULL DEFAULT 10,
    deleted     boolean      NOT NULL DEFAULT false
);
CREATE INDEX idx_prj_template_folder ON prj_template_folder (template_id) WHERE deleted = false;

-- 项目文档目录（立项时由模板生成；后续文档归档挂到这里）
CREATE TABLE prj_project_folder (
    id          bigint       PRIMARY KEY,
    company_id  bigint       NOT NULL,
    project_id  bigint       NOT NULL,
    parent_id   bigint,
    path        varchar(1000) NOT NULL,                     -- 物化路径 /父id/本id/
    name        varchar(200) NOT NULL,
    sort_no     int          NOT NULL DEFAULT 10,
    created_by  bigint,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    deleted     boolean      NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_prj_project_folder_path ON prj_project_folder (path) WHERE deleted = false;
CREATE INDEX idx_prj_project_folder_tree
    ON prj_project_folder (project_id, parent_id, sort_no) WHERE deleted = false;
