-- =====================================================================
-- 零部件分类（T-H-01a）｜分类关联编码：按编码自动匹配分类（台账 B14 ①）
--
-- 蓝本取证：
--   TSDC/PSE/Js/Classify.js:117-127：零件无分类时，调 GetPathByCode(编码, 对象类型)
--     取 GENERATORPATH，再调 ClassifyEditService.GetClassByPathName(GENERATORPATH, 类型)
--     命中即自动归类；
--   InforCenter/PDM/Classify/Edit/Config/Model/CLASSIFYCLASS.dic:52-75：分类上
--     GENERATORPATH/CODEPATH/CLASSCODEPATH 三列的中文标签均为「关联编码」。
--
-- 新系统等价方式（SUBSTITUTED）：
--   蓝本的“编码生成器路径”在新系统没有对应物，等价为「分类关联一条 code_rule」；
--   归类建议 = 按对象编码匹配到规则 → 取该规则关联的分类。
--   一个规则在同一对象类型下只允许关联一个分类（由服务层保证），避免建议歧义。
-- =====================================================================

ALTER TABLE doc_class ADD COLUMN IF NOT EXISTS code_rule_code varchar(64);

CREATE INDEX IF NOT EXISTS idx_doc_class_code_rule
    ON doc_class (entity_code, code_rule_code)
    WHERE deleted = false AND code_rule_code IS NOT NULL;
