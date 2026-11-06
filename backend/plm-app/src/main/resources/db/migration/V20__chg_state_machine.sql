-- =====================================================================
-- P5-8 变更管理三套阶段状态机改为元数据定义（P5 任务 7）
--
-- 背景：V18 建表时状态机写死在 Java 常量里（MAIN_EDITABLE / CLOSABLE 等），
--       调整流程必须改代码。本迁移把 ECR / ECN / Problem 三套状态机
--       落到既有元数据表 meta_lifecycle（type='CHG'），运行时由
--       ChgStateMachine 读取 definition_json 判定迁移。
--
-- 定义格式（与 P1 生命周期定义同构，多出来的只有 ECR 的补充状态）：
--   {
--     "initial": "DRAFT",
--     "states":      [{"code","name"}],
--     "transitions": [{"from","to","action","label"}]
--   }
--
-- 约定：
--   1. 「能否编辑主档」= 状态等于 initial（草稿）；
--   2. 「能否调整关联对象」= 状态等于 initial，或它是 submit/review 迁移的终点；
--   3. from == to 的自迁移表示「该状态允许此动作但不改状态」（如已通知状态下再下发通知）；
--   4. action 用英文小写动词，与业务代码调用点一一对应（submit/review/approve/reject/
--      withdraw/notice/execute/close/issue/cancel）。
-- =====================================================================

INSERT INTO meta_lifecycle (id, code, name, type, definition_json, version_no, status)
VALUES
    (151001, 'CHG_ECR', '变更申请状态机', 'CHG', $json$
{
  "initial": "DRAFT",
  "states": [
    {"code": "DRAFT",     "name": "草稿"},
    {"code": "SUBMITTED", "name": "已提交"},
    {"code": "REVIEWING", "name": "评审中"},
    {"code": "APPROVED",  "name": "已批准"},
    {"code": "REJECTED",  "name": "已驳回"},
    {"code": "NOTICED",   "name": "已通知"},
    {"code": "EXECUTING", "name": "执行中"},
    {"code": "CLOSED",    "name": "已关闭"},
    {"code": "CANCELLED", "name": "已取消"}
  ],
  "transitions": [
    {"from": "DRAFT",     "to": "SUBMITTED", "action": "submit",   "label": "提交评审"},
    {"from": "SUBMITTED", "to": "REVIEWING", "action": "review",   "label": "进入评审"},
    {"from": "SUBMITTED", "to": "APPROVED",  "action": "approve",  "label": "审批通过"},
    {"from": "REVIEWING", "to": "APPROVED",  "action": "approve",  "label": "审批通过"},
    {"from": "SUBMITTED", "to": "DRAFT",     "action": "withdraw", "label": "撤回"},
    {"from": "REVIEWING", "to": "DRAFT",     "action": "withdraw", "label": "撤回"},
    {"from": "REVIEWING", "to": "REJECTED",  "action": "reject",   "label": "评审不通过"},
    {"from": "APPROVED",  "to": "NOTICED",   "action": "notice",   "label": "下发变更通知"},
    {"from": "NOTICED",   "to": "NOTICED",   "action": "notice",   "label": "再次下发变更通知"},
    {"from": "EXECUTING", "to": "EXECUTING", "action": "notice",   "label": "执行中补发变更通知"},
    {"from": "APPROVED",  "to": "EXECUTING", "action": "execute",  "label": "执行变更"},
    {"from": "NOTICED",   "to": "EXECUTING", "action": "execute",  "label": "执行变更"},
    {"from": "EXECUTING", "to": "EXECUTING", "action": "execute",  "label": "继续执行变更"},
    {"from": "APPROVED",  "to": "CLOSED",    "action": "close",    "label": "关闭"},
    {"from": "NOTICED",   "to": "CLOSED",    "action": "close",    "label": "关闭"},
    {"from": "EXECUTING", "to": "CLOSED",    "action": "close",    "label": "关闭"},
    {"from": "DRAFT",     "to": "CANCELLED", "action": "cancel",   "label": "取消"}
  ]
}
$json$::jsonb, 1, 'PUBLISHED'),

    (151002, 'CHG_ECN', '变更通知状态机', 'CHG', $json$
{
  "initial": "DRAFT",
  "states": [
    {"code": "DRAFT",     "name": "草稿"},
    {"code": "ISSUED",    "name": "已签发"},
    {"code": "EXECUTING", "name": "执行中"},
    {"code": "CLOSED",    "name": "已关闭"}
  ],
  "transitions": [
    {"from": "DRAFT",     "to": "ISSUED",    "action": "issue",   "label": "签发"},
    {"from": "ISSUED",    "to": "EXECUTING", "action": "execute", "label": "开始执行"},
    {"from": "ISSUED",    "to": "CLOSED",    "action": "close",   "label": "关闭"},
    {"from": "EXECUTING", "to": "CLOSED",    "action": "close",   "label": "关闭"}
  ]
}
$json$::jsonb, 1, 'PUBLISHED'),

    (151003, 'CHG_PROBLEM', '变更问题状态机', 'CHG', $json$
{
  "initial": "OPEN",
  "states": [
    {"code": "OPEN",       "name": "待处理"},
    {"code": "PROCESSING", "name": "处理中"},
    {"code": "CLOSED",     "name": "已关闭"}
  ],
  "transitions": [
    {"from": "OPEN",       "to": "PROCESSING", "action": "start", "label": "开始处理"},
    {"from": "OPEN",       "to": "CLOSED",     "action": "close", "label": "关闭"},
    {"from": "PROCESSING", "to": "CLOSED",     "action": "close", "label": "关闭"}
  ]
}
$json$::jsonb, 1, 'PUBLISHED')
ON CONFLICT DO NOTHING;
