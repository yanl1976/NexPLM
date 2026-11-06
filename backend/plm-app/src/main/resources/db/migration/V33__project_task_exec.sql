-- =====================================================================
-- P7-3 任务执行闭环：派工 → 接收 → 开始 → 提交审核 → 通过/打回
--
-- 不新增状态列，直接扩展 prj_task.status 的取值：
--   NOT_STARTED →(start) IN_PROGRESS →(submit) SUBMITTED →(approve) DONE
--                                              ↓(reject)
--                                          IN_PROGRESS（保留 reject_reason）
--   BLOCKED 仍需人工指定（受阻），可从任意非终态进入。
--
-- 责任人口径：prj_task.owner_id 即「执行人」，派工（assign）就是设置它并盖 assigned_at；
-- 接收/开始/提交换人做会留下时间戳断层，因此每个动作都记独立时间戳（审计与效率分析都要用）。
-- =====================================================================

ALTER TABLE prj_task ADD COLUMN assigned_at   timestamptz;
ALTER TABLE prj_task ADD COLUMN accepted_at   timestamptz;
ALTER TABLE prj_task ADD COLUMN submitted_at  timestamptz;
ALTER TABLE prj_task ADD COLUMN approved_at   timestamptz;
ALTER TABLE prj_task ADD COLUMN submit_note   varchar(500);
ALTER TABLE prj_task ADD COLUMN reject_reason varchar(500);

COMMENT ON COLUMN prj_task.assigned_at IS '派工时间（设置执行人时）';
COMMENT ON COLUMN prj_task.accepted_at IS '执行人接收时间';
COMMENT ON COLUMN prj_task.submitted_at IS '提交审核时间';
COMMENT ON COLUMN prj_task.approved_at IS '审核通过时间';

-- 「我的任务」按执行人 + 状态翻页
CREATE INDEX idx_prj_task_executor ON prj_task (company_id, owner_id, status) WHERE deleted = false;
