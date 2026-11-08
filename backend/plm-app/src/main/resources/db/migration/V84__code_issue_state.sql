-- =====================================================================
-- 编码发放流水状态（CODE-04，EXP-20261108-56）
--
-- 蓝本 `EditItem.js` 换编码时调 `SetCodeState({ State: "LoseEfficacy" })`：
-- **旧码置为失效，不删不改历史**。新系统原先只记发放、没有状态，
-- 换码后旧码仍像"有效"，追溯会歧义。
--
-- 状态取值（当前只落两种，其余待蓝本反编译确认）：
--   ACTIVE        正常发放
--   LOSE_EFFICACY 已失效（换码）
-- =====================================================================

ALTER TABLE code_issue ADD COLUMN IF NOT EXISTS state varchar(32) NOT NULL DEFAULT 'ACTIVE';

COMMENT ON COLUMN code_issue.state IS
    '发放状态：ACTIVE 正常 / LOSE_EFFICACY 已失效（换码时置位，对应蓝本 CODERES 状态）';

CREATE INDEX IF NOT EXISTS idx_code_issue_state ON code_issue (state);
