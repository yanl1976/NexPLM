-- =====================================================================
-- 编码发放流水（T-CORE-02）
--
-- 背景：蓝本 InforCenter/Coding 的 CODERES 表 60,300 行，记录「谁在什么时候
--   按哪条规则给哪个对象类型发了什么编码」；新系统只有 code_rule.current_no
--   （当前计数）与 code_rule_audit（规则变更），**没有逐次发放记录**，
--   无法回答「PN-000123 是什么时候发的、发给谁」。
--
-- 设计：
--   1. 只追加、不更新（发放是事实，不可改）；
--   2. **不加唯一索引**：管理员重置/跳号后可能重发同一编码，那是合法业务，
--      流水应完整记录每一次发放（而不是把第二次吞掉）；查询索引按 (rule_code, code) 建；
--   3. object_type 记「细分类型」（如 PdmItem:PART），便于按物料类型追溯；
--   4. issued_by 允许为空（定时任务/测试无登录上下文时）；查询时左连 sys_user 取名。
-- =====================================================================

CREATE TABLE IF NOT EXISTS code_issue (
    id          bigint       PRIMARY KEY,
    rule_code   varchar(64)  NOT NULL,
    code        varchar(64)  NOT NULL,
    object_type varchar(64),
    issued_by   bigint,
    issued_at   timestamptz  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_code_issue_rule_code ON code_issue (rule_code, code);
CREATE INDEX IF NOT EXISTS idx_code_issue_time ON code_issue (issued_at DESC);
CREATE INDEX IF NOT EXISTS idx_code_issue_type ON code_issue (object_type, issued_at DESC);
