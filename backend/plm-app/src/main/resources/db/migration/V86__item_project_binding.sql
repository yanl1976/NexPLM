-- =====================================================================
-- 物料 ↔ 项目绑定 + 项目文件夹映射（EXP-20261108-58）
--
-- PLM 的工作区模型（库里已有，不需新建）：
--   ws_folder         个人/公共工作区文件夹（space_type = MY / PUBLIC，owner_id）
--   ws_folder_content 文件夹里放了什么对象（folder_id → object_id）
--   prj_project       项目
--   prj_project_folder 项目文件夹树（project_id / parent_id / path）
--
-- 缺的两块：
--   1) item_master.project_id —— 物料归属哪个项目
--   2) prj_project_content    —— 项目文件夹里放了哪些对象（项目侧映射）
--
-- 新建物料时同时写两份映射：
--   个人空间/{项目编码}/物料  → ws_folder_content
--   {项目}/物料               → prj_project_content
-- 审批归档时再把对象挂到项目归档文件夹（同一张映射表）。
-- =====================================================================

ALTER TABLE item_master ADD COLUMN IF NOT EXISTS project_id bigint;

COMMENT ON COLUMN item_master.project_id IS '所属项目（prj_project.id）；为空表示未绑定项目';

CREATE INDEX IF NOT EXISTS idx_item_master_project ON item_master (project_id);

CREATE TABLE IF NOT EXISTS prj_project_content (
    id                bigint      PRIMARY KEY,
    project_folder_id bigint      NOT NULL,
    object_id         bigint      NOT NULL,
    created_by        bigint,
    created_at        timestamptz NOT NULL DEFAULT now()
);

COMMENT ON TABLE prj_project_content IS '项目文件夹内容映射（项目侧）：文件夹 → 对象';
COMMENT ON COLUMN prj_project_content.project_folder_id IS 'prj_project_folder.id';

CREATE UNIQUE INDEX IF NOT EXISTS uk_prj_project_content
    ON prj_project_content (project_folder_id, object_id);
CREATE INDEX IF NOT EXISTS idx_prj_project_content_object
    ON prj_project_content (object_id);
