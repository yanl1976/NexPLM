-- =====================================================================
-- 071 编码码段（V83 之后执行；依赖 060 已导入 code_kind）
--
-- 来源：蓝本 CODEKINDTORULE + CODERULE（只读导出，12 条 = 4 个种类 × 3 段）
--   元器件   大类 ord1 len0 / 小类 ord2 len0（父=大类）/ 流水码 ord3 len5
--   原材料   同上
--   外购标准件 同上
--   成品     同上
-- 幂等：按 (rule_code, code) 冲突跳过。
-- =====================================================================

-- 元器件编码规则
INSERT INTO code_segment (id, rule_code, code, name, order_no, length, parent_code, drop_mode)
SELECT 9300000000001, k.rule_code, 'SEG01', '大类', 1, 0, NULL, 'DROPDOWN'
  FROM code_kind k WHERE k.legacy_id = 'CODEKIND_61a20f96312e419cbc8f61b903049edb'
ON CONFLICT (rule_code, code) DO NOTHING;

INSERT INTO code_segment (id, rule_code, code, name, order_no, length, parent_code, drop_mode)
SELECT 9300000000002, k.rule_code, 'SEG02', '小类', 2, 0, 'SEG01', 'DROPDOWN'
  FROM code_kind k WHERE k.legacy_id = 'CODEKIND_61a20f96312e419cbc8f61b903049edb'
ON CONFLICT (rule_code, code) DO NOTHING;

INSERT INTO code_segment (id, rule_code, code, name, order_no, length, parent_code, drop_mode)
SELECT 9300000000003, k.rule_code, 'SEG03', '流水码', 3, 5, NULL, 'DROPDOWN'
  FROM code_kind k WHERE k.legacy_id = 'CODEKIND_61a20f96312e419cbc8f61b903049edb'
ON CONFLICT (rule_code, code) DO NOTHING;

-- 原材料编码规则
INSERT INTO code_segment (id, rule_code, code, name, order_no, length, parent_code, drop_mode)
SELECT 9300000000004, k.rule_code, 'SEG01', '大类', 1, 0, NULL, 'DROPDOWN'
  FROM code_kind k WHERE k.legacy_id = 'CODEKIND_05ca91aa096c43bfaec2c62c1edd3987'
ON CONFLICT (rule_code, code) DO NOTHING;

INSERT INTO code_segment (id, rule_code, code, name, order_no, length, parent_code, drop_mode)
SELECT 9300000000005, k.rule_code, 'SEG02', '小类', 2, 0, 'SEG01', 'DROPDOWN'
  FROM code_kind k WHERE k.legacy_id = 'CODEKIND_05ca91aa096c43bfaec2c62c1edd3987'
ON CONFLICT (rule_code, code) DO NOTHING;

INSERT INTO code_segment (id, rule_code, code, name, order_no, length, parent_code, drop_mode)
SELECT 9300000000006, k.rule_code, 'SEG03', '流水码', 3, 5, NULL, 'DROPDOWN'
  FROM code_kind k WHERE k.legacy_id = 'CODEKIND_05ca91aa096c43bfaec2c62c1edd3987'
ON CONFLICT (rule_code, code) DO NOTHING;

-- 外购标准件编码规则
INSERT INTO code_segment (id, rule_code, code, name, order_no, length, parent_code, drop_mode)
SELECT 9300000000007, k.rule_code, 'SEG01', '大类', 1, 0, NULL, 'DROPDOWN'
  FROM code_kind k WHERE k.legacy_id = 'CODEKIND_39f8cd293eb940ca8a1f6b5155edf95d'
ON CONFLICT (rule_code, code) DO NOTHING;

INSERT INTO code_segment (id, rule_code, code, name, order_no, length, parent_code, drop_mode)
SELECT 9300000000008, k.rule_code, 'SEG02', '小类', 2, 0, 'SEG01', 'DROPDOWN'
  FROM code_kind k WHERE k.legacy_id = 'CODEKIND_39f8cd293eb940ca8a1f6b5155edf95d'
ON CONFLICT (rule_code, code) DO NOTHING;

INSERT INTO code_segment (id, rule_code, code, name, order_no, length, parent_code, drop_mode)
SELECT 9300000000009, k.rule_code, 'SEG03', '流水码', 3, 5, NULL, 'DROPDOWN'
  FROM code_kind k WHERE k.legacy_id = 'CODEKIND_39f8cd293eb940ca8a1f6b5155edf95d'
ON CONFLICT (rule_code, code) DO NOTHING;

-- 成品编码规则
INSERT INTO code_segment (id, rule_code, code, name, order_no, length, parent_code, drop_mode)
SELECT 9300000000010, k.rule_code, 'SEG01', '大类', 1, 0, NULL, 'DROPDOWN'
  FROM code_kind k WHERE k.legacy_id = 'CODEKIND_e157a0b00f4742b2921f9e523011c951'
ON CONFLICT (rule_code, code) DO NOTHING;

INSERT INTO code_segment (id, rule_code, code, name, order_no, length, parent_code, drop_mode)
SELECT 9300000000011, k.rule_code, 'SEG02', '小类', 2, 0, 'SEG01', 'DROPDOWN'
  FROM code_kind k WHERE k.legacy_id = 'CODEKIND_e157a0b00f4742b2921f9e523011c951'
ON CONFLICT (rule_code, code) DO NOTHING;

INSERT INTO code_segment (id, rule_code, code, name, order_no, length, parent_code, drop_mode)
SELECT 9300000000012, k.rule_code, 'SEG03', '流水码', 3, 5, NULL, 'DROPDOWN'
  FROM code_kind k WHERE k.legacy_id = 'CODEKIND_e157a0b00f4742b2921f9e523011c951'
ON CONFLICT (rule_code, code) DO NOTHING;
