-- =====================================================================
-- 批次 B2｜把既有流水从 code_sequence 继承到 code_rule，避免切换后编码重号
--
-- 背景：8 处取号（CappRouting/CappCard/CappCardTemplate/CappCardVolume/
--   CappQualityPlan/CappQualityItem/CappTimeRule/CappErpDispatch）原先用
--   code_sequence 计数，格式硬编码；V56 预置的规则 current_no 一律是 0。
--   若直接切到 CodeRuleService.next()，第一次取号会从 1 开始，与库里已有编码重号。
--   所以切换前必须把当前计数搬过来。
--
-- 说明：只回填 scope_key='DEFAULT' 的计数（新引擎无 scope 概念，规则级单计数器）；
--   若某规则在 code_sequence 里有多条 scope，取最大值，保证不低于任何已用流水。
-- =====================================================================

UPDATE code_rule r
   SET current_no = COALESCE((
           SELECT MAX(s.current_no) FROM code_sequence s
            WHERE s.rule_code = r.code
       ), r.current_no),
       updated_at = now()
 WHERE r.current_no = 0
   AND EXISTS (SELECT 1 FROM code_sequence s WHERE s.rule_code = r.code);
