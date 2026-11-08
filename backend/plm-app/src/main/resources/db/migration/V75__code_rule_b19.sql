-- =====================================================================
-- 批次 B 收尾｜B19：把"本地计数 + 硬编码格式"的取号接入 code_rule
--
-- 背景（docs/03 B19、docs/06 EXP-20261108-15/18）：
--   蓝本 InforCenter/Coding 是统一取号服务（CODERULE 58 段 + CODERES 已发放记录）；
--   新系统已有 code_rule 引擎，但只有 V56 预置的 10 条规则接上，其余 15 个 ruleCode
--   仍各自用 code_sequence + String.format。本迁移只补规则定义，段格式严格对齐现有输出，
--   代码切换由同一批次的服务改动完成（必须整批切，见 docs/03 B19 的切换约束）。
--
-- 回填：与 V57 同一口径（MAX(code_sequence.current_no)）。**必须与代码切换同批发布**，
--   否则未切服务继续增长 code_sequence，后切服务第一次取号会从 1 开始、与库内已有编码重号。
--
-- 不在本迁移范围：ReportService 的 RPT-（它是 count(*)+1 取号、不走 code_sequence，
--   需单独设计初始化方式，见 docs/03 B19）。
--
-- 注意：QltIssue 的 QI- 与 V56 的 CappQualityItem（工艺质量控制项）前缀相同，
--   但两者落在不同表（质量问题 vs 质量控制项），不构成同表重号；沿用既有前缀以免改变用户可见编码。
-- =====================================================================

INSERT INTO code_rule (id, code, name, object_type, segments_json, reset_policy, status, remark) VALUES
    (920001, 'ChgEcr', '变更申请（ECR）', 'ChgEcr',
     '[{"type":"TEXT","value":"ECR-"},{"type":"SEQ","width":6,"radix":"DECIMAL"}]', 'NEVER', 'ACTIVE', 'B19：对齐原 ECR-%06d'),
    (920002, 'ChgEcn', '变更通知（ECN）', 'ChgEcn',
     '[{"type":"TEXT","value":"ECN-"},{"type":"SEQ","width":6,"radix":"DECIMAL"}]', 'NEVER', 'ACTIVE', 'B19：对齐原 ECN-%06d'),
    (920003, 'ChgProblem', '变更问题', 'ChgProblem',
     '[{"type":"TEXT","value":"PBM-"},{"type":"SEQ","width":6,"radix":"DECIMAL"}]', 'NEVER', 'ACTIVE', 'B19：对齐原 PBM-%06d'),
    (920004, 'QltAuditPlan', '体系审核计划', 'QltAuditPlan',
     '[{"type":"TEXT","value":"AU-"},{"type":"SEQ","width":6,"radix":"DECIMAL"}]', 'NEVER', 'ACTIVE', 'B19：对齐原 AU-%06d'),
    (920005, 'QltAuditFinding', '体系审核发现', 'QltAuditFinding',
     '[{"type":"TEXT","value":"FD-"},{"type":"SEQ","width":6,"radix":"DECIMAL"}]', 'NEVER', 'ACTIVE', 'B19：对齐原 FD-%06d'),
    (920006, 'PrjProject', '项目', 'PrjProject',
     '[{"type":"TEXT","value":"PJ-"},{"type":"SEQ","width":6,"radix":"DECIMAL"}]', 'NEVER', 'ACTIVE', 'B19：对齐原 PJ-%06d'),
    (920007, 'PrjTemplate', '项目模板', 'PrjTemplate',
     '[{"type":"TEXT","value":"PT-"},{"type":"SEQ","width":6,"radix":"DECIMAL"}]', 'NEVER', 'ACTIVE', 'B19：对齐原 PT-%06d'),
    (920008, 'ItgJob', '集成下发任务', 'ItgJob',
     '[{"type":"TEXT","value":"ITG-"},{"type":"SEQ","width":6,"radix":"DECIMAL"}]', 'NEVER', 'ACTIVE', 'B19：对齐原 ITG-%06d'),
    (920009, 'QltIssue', '质量问题', 'QltIssue',
     '[{"type":"TEXT","value":"QI-"},{"type":"SEQ","width":6,"radix":"DECIMAL"}]', 'NEVER', 'ACTIVE', 'B19：对齐原 QI-%06d'),
    (920010, 'QltTemplate', '8D 模板', 'QltTemplate',
     '[{"type":"TEXT","value":"DT-"},{"type":"SEQ","width":6,"radix":"DECIMAL"}]', 'NEVER', 'ACTIVE', 'B19：对齐原 DT-%06d'),
    (920011, 'SysDocTemplate', '体系文件模板', 'SysDocTemplate',
     '[{"type":"TEXT","value":"TPL-"},{"type":"SEQ","width":6,"radix":"DECIMAL"}]', 'NEVER', 'ACTIVE', 'B19：对齐原 TPL-%06d'),
    (920012, 'SupSupplier', '供应商', 'SupSupplier',
     '[{"type":"TEXT","value":"SUP-"},{"type":"SEQ","width":6,"radix":"DECIMAL"}]', 'NEVER', 'ACTIVE', 'B19：对齐原 SUP-%06d'),
    (920013, 'SupAdmission', '供应商准入', 'SupAdmission',
     '[{"type":"TEXT","value":"ADM-"},{"type":"SEQ","width":6,"radix":"DECIMAL"}]', 'NEVER', 'ACTIVE', 'B19：对齐原 ADM-%06d'),
    (920014, 'SupIssuance', '供应商发放', 'SupIssuance',
     '[{"type":"TEXT","value":"ISS-"},{"type":"SEQ","width":6,"radix":"DECIMAL"}]', 'NEVER', 'ACTIVE', 'B19：对齐原 ISS-%06d'),
    (920015, 'SupRateScheme', '供应商评级方案', 'SupRateScheme',
     '[{"type":"TEXT","value":"RATE-"},{"type":"SEQ","width":6,"radix":"DECIMAL"}]', 'NEVER', 'ACTIVE', 'B19：对齐原 RATE-%06d'),
    (920016, 'ReportTemplate', '自建报表模板', 'ReportTemplate',
     '[{"type":"TEXT","value":"RPT-"},{"type":"SEQ","width":6,"radix":"DECIMAL"}]', 'NEVER', 'ACTIVE', 'B19：对齐原 RPT-%06d')
ON CONFLICT DO NOTHING;

-- 回填当前流水（V57 同口径）：切换前执行，避免从 1 开始重号
UPDATE code_rule r
   SET current_no = COALESCE((
           SELECT MAX(s.current_no) FROM code_sequence s
            WHERE s.rule_code = r.code
       ), r.current_no),
       updated_at = now()
 WHERE r.current_no = 0
   AND EXISTS (SELECT 1 FROM code_sequence s WHERE s.rule_code = r.code);

-- 自建报表模板不走 code_sequence（原实现是 COUNT(*)+1 加冲突探测），
-- 按既有 RPT- 纯数字编码的最大值回填；内置可读编码（RPT-DOC-LEDGER 等）不在范围内。
UPDATE code_rule r
   SET current_no = COALESCE((
           SELECT MAX(CAST(SUBSTRING(code FROM 5) AS bigint))
             FROM rpt_template
            WHERE code ~ '^RPT-[0-9]{6}$'
       ), 0),
       updated_at = now()
 WHERE r.code = 'ReportTemplate'
   AND r.current_no = 0;
