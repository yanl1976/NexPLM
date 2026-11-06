-- =====================================================================
-- P7-4 交付物与齐套性检查
--
-- 建模沿用旧系统 prj_task_output：一个任务对多个「对象」，靠 output_type 区分输入/输出。
--   INPUT  输入：这个任务开工前要拿到的上游成果（追溯用，不参与齐套判定）
--   OUTPUT 输出：这个任务要产出的成果（齐套判定只认 required=true 的 OUTPUT）
--
-- 对象是多态的（object_type + object_id），不建外键——三类对象分属三张表：
--   DOC       → plm_object.id（object_type = 'PdmDoc'）
--   ITEM      → plm_object.id（object_type = 'PdmItem'）
--   CAPP_CARD → capp_card.id
-- 因此不存 code/name 快照：对象改名后交付物跟着变，避免两份数据不一致。
--
-- 齐套口径（FAIL 要极窄，否则用户会无视红叉）：
--   MISSING   对象已删除/不存在      → FAIL（必需输出项缺失）
--   OBSOLETE  lifecycle_state=Obsolete → FAIL（成果已废弃，等于没有）
--   UNRELEASED 存在但未 Released      → WARN（活干到一半，不该拦）
--   OK        已发布 / 工艺卡片        → 通过
-- =====================================================================

CREATE TABLE prj_task_output (
    id           bigint       PRIMARY KEY,
    company_id   bigint       NOT NULL,
    task_id      bigint       NOT NULL,
    output_type  varchar(8)   NOT NULL DEFAULT 'OUTPUT',   -- INPUT / OUTPUT
    object_type  varchar(16)  NOT NULL,                    -- DOC / ITEM / CAPP_CARD
    object_id    bigint       NOT NULL,
    required     boolean      NOT NULL DEFAULT true,        -- 是否齐套必需（仅对 OUTPUT 有意义）
    remark       varchar(500),
    created_by   bigint,
    created_at   timestamptz  NOT NULL DEFAULT now(),
    deleted      boolean      NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_prj_task_output
    ON prj_task_output (task_id, output_type, object_type, object_id) WHERE deleted = false;
CREATE INDEX idx_prj_task_output_task ON prj_task_output (task_id) WHERE deleted = false;
CREATE INDEX idx_prj_task_output_object ON prj_task_output (object_type, object_id);
