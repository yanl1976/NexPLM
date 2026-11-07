-- =====================================================================
-- 批次 B｜编码管理（对齐旧系统 InforCenter/Coding 的编码规则维护）
--
-- 旧系统模型取证（PLMSOFT/Server/InforCenter/Coding/Config/CodeRule/Coding.coderule）：
--   编码 = 若干「段」顺序拼接，段类型（ThickRule）5 类：
--     Number / Char / NumberOrChar / Date(yyyy|yyyyMM|yyyyMMdd) / OrderCode(流水码，0-9 或 0-9A-Z)
--
-- 注意：`code_rule` **在 V1 基线里已经存在**（id/code/name/segments_json/scope_json/
--   reset_policy/status/version_no），只是从未被任何代码使用，编码格式硬编码在 8 处
--   `String.format("%s-%06d", prefix, no)`。故本迁移只做 ALTER + 补种子，不重建表——
--   重建会破坏既有约束名 `uk_code_rule_code` 与潜在的历史数据。
-- =====================================================================

-- 计数器直接落在规则行上（旧系统也是"每条规则一个当前流水"）
ALTER TABLE code_rule ADD COLUMN IF NOT EXISTS current_no  bigint      NOT NULL DEFAULT 0;
-- 已归零到的期间（yyyy / yyyyMM / yyyyMMdd），配合 reset_policy 判断是否需要归零
ALTER TABLE code_rule ADD COLUMN IF NOT EXISTS last_period varchar(16);
ALTER TABLE code_rule ADD COLUMN IF NOT EXISTS object_type varchar(64);
ALTER TABLE code_rule ADD COLUMN IF NOT EXISTS remark      varchar(500);

-- 老默认值 NONE 统一成 NEVER（取值集合：NEVER/YEAR/MONTH/DAY）
ALTER TABLE code_rule ALTER COLUMN reset_policy SET DEFAULT 'NEVER';
UPDATE code_rule SET reset_policy = 'NEVER' WHERE reset_policy IN ('NONE', '');

-- 跳号/重置审计：编码连续性属审计关注点（谁、从几到几、为什么）
CREATE TABLE IF NOT EXISTS code_rule_audit (
    id          bigint      PRIMARY KEY,
    rule_code   varchar(64) NOT NULL,
    action      varchar(16) NOT NULL,   -- RESET / SKIP / UPDATE_SEGMENTS
    from_no     bigint,
    to_no       bigint,
    operator_id bigint,
    reason      varchar(500),
    created_at  timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_code_rule_audit ON code_rule_audit (rule_code, created_at DESC);

-- 预置既有 10 条规则：段定义与当前硬编码的 prefix 完全一致（行为不变，只是把格式搬进表）
INSERT INTO code_rule (id, code, name, object_type, segments_json, reset_policy, status, remark) VALUES
    (910001, 'PdmDoc', '图文档', 'PdmDoc',
     '[{"type":"TEXT","value":"DOC-"},{"type":"SEQ","width":6,"radix":"DECIMAL"}]', 'NEVER', 'ACTIVE', '预置：与既有生成规则一致'),
    (910002, 'PdmItem', '零部件', 'PdmItem',
     '[{"type":"TEXT","value":"PN-"},{"type":"SEQ","width":6,"radix":"DECIMAL"}]', 'NEVER', 'ACTIVE', '预置：与既有生成规则一致'),
    (910003, 'CappRouting', '工艺路线', 'CappRouting',
     '[{"type":"TEXT","value":"RT-"},{"type":"SEQ","width":6,"radix":"DECIMAL"}]', 'NEVER', 'ACTIVE', '预置'),
    (910004, 'CappCard', '工艺卡片', 'CappCard',
     '[{"type":"TEXT","value":"CC-"},{"type":"SEQ","width":6,"radix":"DECIMAL"}]', 'NEVER', 'ACTIVE', '预置'),
    (910005, 'CappCardTemplate', '工艺卡模板', 'CappCardTemplate',
     '[{"type":"TEXT","value":"CT-"},{"type":"SEQ","width":6,"radix":"DECIMAL"}]', 'NEVER', 'ACTIVE', '预置'),
    (910006, 'CappCardVolume', '工艺成册', 'CappCardVolume',
     '[{"type":"TEXT","value":"CV-"},{"type":"SEQ","width":6,"radix":"DECIMAL"}]', 'NEVER', 'ACTIVE', '预置'),
    (910007, 'CappQualityPlan', '质量规划', 'CappQualityPlan',
     '[{"type":"TEXT","value":"QP-"},{"type":"SEQ","width":6,"radix":"DECIMAL"}]', 'NEVER', 'ACTIVE', '预置'),
    (910008, 'CappQualityItem', '质量控制项', 'CappQualityItem',
     '[{"type":"TEXT","value":"QI-"},{"type":"SEQ","width":6,"radix":"DECIMAL"}]', 'NEVER', 'ACTIVE', '预置'),
    (910009, 'CappTimeRule', '工时定额', 'CappTimeRule',
     '[{"type":"TEXT","value":"TR-"},{"type":"SEQ","width":6,"radix":"DECIMAL"}]', 'NEVER', 'ACTIVE', '预置'),
    (910010, 'CappErpDispatch', 'ERP下发任务', 'CappErpDispatch',
     '[{"type":"TEXT","value":"ED-"},{"type":"SEQ","width":6,"radix":"DECIMAL"}]', 'NEVER', 'ACTIVE', '预置');
