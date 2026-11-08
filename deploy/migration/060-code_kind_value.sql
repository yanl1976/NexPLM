-- =====================================================================
-- 编码种类 + 码值 + 分类路径迁移：蓝本 CODEKIND/CODEVALUE/CLASSCODEPATH → 新系统三层
-- 生成器：tools/Gen-CodeKindMigration.ps1（只读蓝本，仅生成 SQL，不执行）
-- 数据源：198.168.6.26,1433/TCPLM；生成时间：2026-11-08 15:52:22
-- 统计：分类路径 258 条；种类 4 个；码值 292 条；解析失败 0 条
-- 口径：
--   1) 只迁"有 CLASSCODEPATH 绑定"的分类（258 条），其余 6 条不自动取号
--   2) 每个种类一条 code_rule：段定义 VALUE,VALUE,SEQ(5)
--   3) code_path 形如 `Kind_<16hex>/1001/02/*`（* 表示流水）
-- 注意：执行前必须先跑 V81（建表）与 010（分类树）。
-- =====================================================================

BEGIN;

-- ---- 1. 编码种类 + 对应规则 ----
INSERT INTO code_rule (id, code, name, segments_json, scope_json, reset_policy, status,
                       version_no, created_at, updated_at, current_no, last_period, object_type, remark)
SELECT 930202, 'Kind_61a20f96312e419c', '元器件编码规则', '[{"type":"VALUE"},{"type":"VALUE"},{"type":"SEQ","width":5,"radix":"DECIMAL"}]'::jsonb, '{}'::jsonb, 'NEVER', 'ACTIVE', 1, now(), now(), 0, NULL, 'PdmItem',
       'EXP-20261108-44：种类 元器件编码规则（蓝本 CODEKIND_61a20f96312e419cbc8f61b903049edb）'
 WHERE NOT EXISTS (SELECT 1 FROM code_rule WHERE code = 'Kind_61a20f96312e419c');
INSERT INTO code_kind (code, name, legacy_id, rule_code, sort)
SELECT 'Kind_61a20f96312e419c', '元器件编码规则', 'CODEKIND_61a20f96312e419cbc8f61b903049edb', 'Kind_61a20f96312e419c', 0
 WHERE NOT EXISTS (SELECT 1 FROM code_kind WHERE code = 'Kind_61a20f96312e419c');
INSERT INTO code_rule (id, code, name, segments_json, scope_json, reset_policy, status,
                       version_no, created_at, updated_at, current_no, last_period, object_type, remark)
SELECT 930203, 'Kind_05ca91aa096c43bf', '原材料编码规则', '[{"type":"VALUE"},{"type":"VALUE"},{"type":"SEQ","width":5,"radix":"DECIMAL"}]'::jsonb, '{}'::jsonb, 'NEVER', 'ACTIVE', 1, now(), now(), 0, NULL, 'PdmItem',
       'EXP-20261108-44：种类 原材料编码规则（蓝本 CODEKIND_05ca91aa096c43bfaec2c62c1edd3987）'
 WHERE NOT EXISTS (SELECT 1 FROM code_rule WHERE code = 'Kind_05ca91aa096c43bf');
INSERT INTO code_kind (code, name, legacy_id, rule_code, sort)
SELECT 'Kind_05ca91aa096c43bf', '原材料编码规则', 'CODEKIND_05ca91aa096c43bfaec2c62c1edd3987', 'Kind_05ca91aa096c43bf', 0
 WHERE NOT EXISTS (SELECT 1 FROM code_kind WHERE code = 'Kind_05ca91aa096c43bf');
INSERT INTO code_rule (id, code, name, segments_json, scope_json, reset_policy, status,
                       version_no, created_at, updated_at, current_no, last_period, object_type, remark)
SELECT 930204, 'Kind_39f8cd293eb940ca', '外购标准件编码规则', '[{"type":"VALUE"},{"type":"VALUE"},{"type":"SEQ","width":5,"radix":"DECIMAL"}]'::jsonb, '{}'::jsonb, 'NEVER', 'ACTIVE', 1, now(), now(), 0, NULL, 'PdmItem',
       'EXP-20261108-44：种类 外购标准件编码规则（蓝本 CODEKIND_39f8cd293eb940ca8a1f6b5155edf95d）'
 WHERE NOT EXISTS (SELECT 1 FROM code_rule WHERE code = 'Kind_39f8cd293eb940ca');
INSERT INTO code_kind (code, name, legacy_id, rule_code, sort)
SELECT 'Kind_39f8cd293eb940ca', '外购标准件编码规则', 'CODEKIND_39f8cd293eb940ca8a1f6b5155edf95d', 'Kind_39f8cd293eb940ca', 0
 WHERE NOT EXISTS (SELECT 1 FROM code_kind WHERE code = 'Kind_39f8cd293eb940ca');
INSERT INTO code_rule (id, code, name, segments_json, scope_json, reset_policy, status,
                       version_no, created_at, updated_at, current_no, last_period, object_type, remark)
SELECT 930205, 'Kind_e157a0b00f4742b2', '成品编码规则', '[{"type":"VALUE"},{"type":"VALUE"},{"type":"SEQ","width":5,"radix":"DECIMAL"}]'::jsonb, '{}'::jsonb, 'NEVER', 'ACTIVE', 1, now(), now(), 0, NULL, 'PdmItem',
       'EXP-20261108-44：种类 成品编码规则（蓝本 CODEKIND_e157a0b00f4742b2921f9e523011c951）'
 WHERE NOT EXISTS (SELECT 1 FROM code_rule WHERE code = 'Kind_e157a0b00f4742b2');
INSERT INTO code_kind (code, name, legacy_id, rule_code, sort)
SELECT 'Kind_e157a0b00f4742b2', '成品编码规则', 'CODEKIND_e157a0b00f4742b2921f9e523011c951', 'Kind_e157a0b00f4742b2', 0
 WHERE NOT EXISTS (SELECT 1 FROM code_kind WHERE code = 'Kind_e157a0b00f4742b2');

-- ---- 2. 码值（value_path 表达码值树） ----
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000002, 'Kind_61a20f96312e419c', '大类', '1001', '1001', '', 1
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1001' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000003, 'Kind_61a20f96312e419c', '小类', '1001/01', '01', '', 2
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1001/01' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000004, 'Kind_61a20f96312e419c', '小类', '1001/02', '02', '', 3
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1001/02' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000005, 'Kind_61a20f96312e419c', '小类', '1001/03', '03', '', 4
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1001/03' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000006, 'Kind_61a20f96312e419c', '小类', '1001/04', '04', '', 5
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1001/04' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000007, 'Kind_61a20f96312e419c', '小类', '1001/05', '05', '', 6
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1001/05' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000008, 'Kind_61a20f96312e419c', '小类', '1001/06', '06', '', 7
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1001/06' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000009, 'Kind_61a20f96312e419c', '小类', '1001/07', '07', '', 8
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1001/07' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000010, 'Kind_61a20f96312e419c', '小类', '1001/08', '08', '08', 9
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1001/08' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000011, 'Kind_61a20f96312e419c', '大类', '1002', '1002', '', 10
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1002' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000012, 'Kind_61a20f96312e419c', '小类', '1002/01', '01', '', 11
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1002/01' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000013, 'Kind_61a20f96312e419c', '小类', '1002/02', '02', '', 12
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1002/02' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000014, 'Kind_61a20f96312e419c', '小类', '1002/03', '03', '', 13
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1002/03' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000015, 'Kind_61a20f96312e419c', '小类', '1002/04', '04', '04', 14
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1002/04' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000016, 'Kind_61a20f96312e419c', '大类', '1003', '1003', '', 15
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1003' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000017, 'Kind_61a20f96312e419c', '小类', '1003/01', '01', '', 16
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1003/01' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000018, 'Kind_61a20f96312e419c', '小类', '1003/02', '02', '', 17
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1003/02' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000019, 'Kind_61a20f96312e419c', '小类', '1003/03', '03', '', 18
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1003/03' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000020, 'Kind_61a20f96312e419c', '小类', '1003/04', '04', '', 19
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1003/04' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000021, 'Kind_61a20f96312e419c', '小类', '1003/05', '05', '', 20
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1003/05' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000022, 'Kind_61a20f96312e419c', '大类', '1004', '1004', '', 21
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1004' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000023, 'Kind_61a20f96312e419c', '小类', '1004/01', '01', '', 22
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1004/01' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000024, 'Kind_61a20f96312e419c', '小类', '1004/02', '02', '', 23
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1004/02' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000025, 'Kind_61a20f96312e419c', '小类', '1004/03', '03', '', 24
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1004/03' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000026, 'Kind_61a20f96312e419c', '小类', '1004/04', '04', '', 25
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1004/04' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000027, 'Kind_61a20f96312e419c', '小类', '1004/05', '05', '', 26
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1004/05' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000028, 'Kind_61a20f96312e419c', '小类', '1004/06', '06', '06', 27
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1004/06' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000029, 'Kind_61a20f96312e419c', '小类', '1004/07', '07', '', 28
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1004/07' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000030, 'Kind_61a20f96312e419c', '小类', '1004/08', '08', '08', 29
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1004/08' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000031, 'Kind_61a20f96312e419c', '大类', '1005', '1005', '', 30
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1005' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000032, 'Kind_61a20f96312e419c', '小类', '1005/01', '01', '', 31
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1005/01' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000033, 'Kind_61a20f96312e419c', '小类', '1005/02', '02', '', 32
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1005/02' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000034, 'Kind_61a20f96312e419c', '小类', '1005/03', '03', '', 33
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1005/03' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000035, 'Kind_61a20f96312e419c', '小类', '1005/04', '04', '', 34
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1005/04' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000036, 'Kind_61a20f96312e419c', '小类', '1005/05', '05', '', 35
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1005/05' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000037, 'Kind_61a20f96312e419c', '小类', '1005/06', '06', '', 36
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1005/06' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000038, 'Kind_61a20f96312e419c', '小类', '1005/07', '07', '', 37
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1005/07' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000039, 'Kind_61a20f96312e419c', '小类', '1005/08', '08', '', 38
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1005/08' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000040, 'Kind_61a20f96312e419c', '小类', '1005/09', '09', '', 39
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1005/09' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000041, 'Kind_61a20f96312e419c', '小类', '1005/10', '10', '', 40
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1005/10' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000042, 'Kind_61a20f96312e419c', '大类', '1006', '1006', '', 41
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1006' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000043, 'Kind_61a20f96312e419c', '小类', '1006/01', '01', '', 42
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1006/01' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000044, 'Kind_61a20f96312e419c', '小类', '1006/02', '02', '', 43
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1006/02' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000045, 'Kind_61a20f96312e419c', '大类', '1007', '1007', '', 44
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1007' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000046, 'Kind_61a20f96312e419c', '小类', '1007/01', '01', '', 45
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1007/01' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000047, 'Kind_61a20f96312e419c', '小类', '1007/02', '02', '', 46
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1007/02' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000048, 'Kind_61a20f96312e419c', '小类', '1007/03', '03', '', 47
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1007/03' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000049, 'Kind_61a20f96312e419c', '小类', '1007/04', '04', '04', 48
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1007/04' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000050, 'Kind_61a20f96312e419c', '大类', '1008', '1008', '', 49
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1008' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000051, 'Kind_61a20f96312e419c', '小类', '1008/01', '01', '', 50
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1008/01' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000052, 'Kind_61a20f96312e419c', '小类', '1008/02', '02', '', 51
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1008/02' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000053, 'Kind_61a20f96312e419c', '小类', '1008/03', '03', '', 52
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1008/03' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000054, 'Kind_61a20f96312e419c', '小类', '1008/04', '04', '04', 53
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1008/04' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000055, 'Kind_61a20f96312e419c', '小类', '1008/05', '05', '05', 54
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1008/05' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000056, 'Kind_61a20f96312e419c', '大类', '1009', '1009', '', 55
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1009' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000057, 'Kind_61a20f96312e419c', '小类', '1009/01', '01', '', 56
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1009/01' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000058, 'Kind_61a20f96312e419c', '小类', '1009/02', '02', '', 57
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1009/02' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000059, 'Kind_61a20f96312e419c', '小类', '1009/03', '03', '', 58
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1009/03' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000060, 'Kind_61a20f96312e419c', '小类', '1009/04', '04', '', 59
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1009/04' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000061, 'Kind_61a20f96312e419c', '小类', '1009/05', '05', '', 60
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1009/05' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000062, 'Kind_61a20f96312e419c', '小类', '1009/06', '06', '', 61
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1009/06' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000063, 'Kind_61a20f96312e419c', '小类', '1009/07', '07', '', 62
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1009/07' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000064, 'Kind_61a20f96312e419c', '小类', '1009/08', '08', '', 63
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1009/08' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000065, 'Kind_61a20f96312e419c', '小类', '1009/09', '09', '', 64
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1009/09' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000066, 'Kind_61a20f96312e419c', '小类', '1009/10', '10', '', 65
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1009/10' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000067, 'Kind_61a20f96312e419c', '小类', '1009/11', '11', '', 66
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1009/11' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000068, 'Kind_61a20f96312e419c', '小类', '1009/12', '12', '', 67
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1009/12' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000069, 'Kind_61a20f96312e419c', '小类', '1009/13', '13', '13', 68
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1009/13' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000070, 'Kind_61a20f96312e419c', '小类', '1009/14', '14', '14', 69
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1009/14' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000071, 'Kind_61a20f96312e419c', '大类', '1010', '1010', '', 70
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1010' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000072, 'Kind_61a20f96312e419c', '小类', '1010/01', '01', '', 71
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1010/01' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000073, 'Kind_61a20f96312e419c', '小类', '1010/02', '02', '', 72
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1010/02' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000074, 'Kind_61a20f96312e419c', '小类', '1010/03', '03', '', 73
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1010/03' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000075, 'Kind_61a20f96312e419c', '小类', '1010/04', '04', '04', 74
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1010/04' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000076, 'Kind_61a20f96312e419c', '小类', '1010/05', '05', '', 75
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1010/05' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000077, 'Kind_61a20f96312e419c', '小类', '1010/06', '06', '06', 76
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1010/06' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000078, 'Kind_61a20f96312e419c', '小类', '1010/07', '07', '07', 77
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1010/07' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000079, 'Kind_61a20f96312e419c', '小类', '1010/08', '08', '08', 78
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1010/08' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000080, 'Kind_61a20f96312e419c', '小类', '1010/09', '09', '09', 79
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1010/09' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000081, 'Kind_61a20f96312e419c', '小类', '1010/10', '10', '10', 80
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1010/10' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000082, 'Kind_61a20f96312e419c', '大类', '1011', '1011', '', 81
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1011' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000083, 'Kind_61a20f96312e419c', '小类', '1011/01', '01', '', 82
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1011/01' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000084, 'Kind_61a20f96312e419c', '小类', '1011/02', '02', '02', 83
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1011/02' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000085, 'Kind_61a20f96312e419c', '小类', '1011/03', '03', '', 84
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1011/03' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000086, 'Kind_61a20f96312e419c', '大类', '1012', '1012', '', 85
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1012' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000087, 'Kind_61a20f96312e419c', '小类', '1012/01', '01', '', 86
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1012/01' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000088, 'Kind_61a20f96312e419c', '小类', '1012/02', '02', '', 87
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1012/02' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000089, 'Kind_61a20f96312e419c', '小类', '1012/03', '03', '', 88
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1012/03' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000090, 'Kind_61a20f96312e419c', '小类', '1012/04', '04', '', 89
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1012/04' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000091, 'Kind_61a20f96312e419c', '小类', '1012/05', '05', '', 90
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1012/05' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000092, 'Kind_61a20f96312e419c', '小类', '1012/06', '06', '', 91
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1012/06' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000093, 'Kind_61a20f96312e419c', '大类', '1013', '1013', '', 92
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1013' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000094, 'Kind_61a20f96312e419c', '小类', '1013/01', '01', '', 93
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1013/01' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000095, 'Kind_61a20f96312e419c', '小类', '1013/02', '02', '', 94
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1013/02' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000096, 'Kind_61a20f96312e419c', '小类', '1013/03', '03', '', 95
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1013/03' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000097, 'Kind_61a20f96312e419c', '小类', '1013/04', '04', '', 96
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1013/04' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000098, 'Kind_61a20f96312e419c', '小类', '1013/05', '05', '', 97
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1013/05' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000099, 'Kind_61a20f96312e419c', '小类', '1013/06', '06', '', 98
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1013/06' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000100, 'Kind_61a20f96312e419c', '小类', '1013/07', '07', '07', 99
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1013/07' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000101, 'Kind_61a20f96312e419c', '小类', '1013/08', '08', '08', 100
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1013/08' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000102, 'Kind_61a20f96312e419c', '大类', '1014', '1014', '', 101
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1014' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000103, 'Kind_61a20f96312e419c', '小类', '1014/01', '01', '', 102
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1014/01' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000104, 'Kind_61a20f96312e419c', '小类', '1014/02', '02', '', 103
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1014/02' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000105, 'Kind_61a20f96312e419c', '小类', '1014/03', '03', '', 104
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1014/03' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000106, 'Kind_61a20f96312e419c', '小类', '1014/04', '04', '', 105
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1014/04' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000107, 'Kind_61a20f96312e419c', '小类', '1014/05', '05', '', 106
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1014/05' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000108, 'Kind_61a20f96312e419c', '小类', '1014/06', '06', '', 107
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1014/06' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000109, 'Kind_61a20f96312e419c', '小类', '1014/07', '07', '', 108
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1014/07' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000110, 'Kind_61a20f96312e419c', '小类', '1014/08', '08', '', 109
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1014/08' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000111, 'Kind_61a20f96312e419c', '小类', '1014/09', '09', '', 110
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1014/09' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000112, 'Kind_61a20f96312e419c', '小类', '1014/10', '10', '10', 111
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1014/10' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000113, 'Kind_61a20f96312e419c', '小类', '1014/11', '11', '11', 112
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1014/11' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000114, 'Kind_61a20f96312e419c', '大类', '1015', '1015', '', 113
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1015' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000115, 'Kind_61a20f96312e419c', '小类', '1015/01', '01', '', 114
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1015/01' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000116, 'Kind_61a20f96312e419c', '小类', '1015/02', '02', '', 115
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1015/02' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000117, 'Kind_61a20f96312e419c', '小类', '1015/03', '03', '', 116
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1015/03' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000118, 'Kind_61a20f96312e419c', '小类', '1015/04', '04', '', 117
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1015/04' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000119, 'Kind_61a20f96312e419c', '小类', '1015/05', '05', '', 118
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1015/05' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000120, 'Kind_61a20f96312e419c', '小类', '1015/06', '06', '', 119
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1015/06' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000121, 'Kind_61a20f96312e419c', '小类', '1015/07', '07', '', 120
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1015/07' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000122, 'Kind_61a20f96312e419c', '小类', '1015/08', '08', '', 121
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1015/08' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000123, 'Kind_61a20f96312e419c', '小类', '1015/09', '09', '', 122
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1015/09' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000124, 'Kind_61a20f96312e419c', '小类', '1015/10', '10', '', 123
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1015/10' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000125, 'Kind_61a20f96312e419c', '小类', '1015/11', '11', '', 124
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1015/11' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000126, 'Kind_61a20f96312e419c', '小类', '1015/12', '12', '', 125
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1015/12' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000127, 'Kind_61a20f96312e419c', '小类', '1015/13', '13', '', 126
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1015/13' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000128, 'Kind_61a20f96312e419c', '小类', '1015/14', '14', '', 127
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1015/14' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000129, 'Kind_61a20f96312e419c', '小类', '1015/15', '15', '15', 128
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1015/15' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000130, 'Kind_61a20f96312e419c', '小类', '1015/16', '16', '16', 129
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1015/16' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000131, 'Kind_61a20f96312e419c', '大类', '1016', '1016', '', 130
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1016' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000132, 'Kind_61a20f96312e419c', '小类', '1016/01', '01', '', 131
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1016/01' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000133, 'Kind_61a20f96312e419c', '小类', '1016/02', '02', '', 132
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1016/02' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000134, 'Kind_61a20f96312e419c', '小类', '1016/03', '03', '', 133
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1016/03' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000135, 'Kind_61a20f96312e419c', '小类', '1016/04', '04', '04', 134
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1016/04' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000136, 'Kind_61a20f96312e419c', '小类', '1016/05', '05', '05', 135
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1016/05' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000137, 'Kind_61a20f96312e419c', '小类', '1016/06', '06', '06', 136
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1016/06' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000138, 'Kind_61a20f96312e419c', '小类', '1016/07', '07', '07', 137
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1016/07' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000139, 'Kind_61a20f96312e419c', '大类', '1017', '1017', '', 138
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1017' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000140, 'Kind_61a20f96312e419c', '小类', '1017/01', '01', '', 139
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1017/01' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000141, 'Kind_61a20f96312e419c', '小类', '1017/02', '02', '', 140
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1017/02' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000142, 'Kind_61a20f96312e419c', '小类', '1017/03', '03', '', 141
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1017/03' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000143, 'Kind_61a20f96312e419c', '小类', '1017/04', '04', '', 142
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1017/04' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000144, 'Kind_61a20f96312e419c', '小类', '1017/05', '05', '', 143
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1017/05' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000145, 'Kind_61a20f96312e419c', '小类', '1017/06', '06', '', 144
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1017/06' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000146, 'Kind_61a20f96312e419c', '大类', '1018', '1018', '', 145
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1018' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000147, 'Kind_61a20f96312e419c', '小类', '1018/01', '01', '', 146
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1018/01' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000148, 'Kind_61a20f96312e419c', '小类', '1018/02', '02', '', 147
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1018/02' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000149, 'Kind_61a20f96312e419c', '小类', '1018/03', '03', '', 148
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1018/03' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000150, 'Kind_61a20f96312e419c', '小类', '1018/04', '04', '', 149
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1018/04' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000151, 'Kind_61a20f96312e419c', '小类', '1018/05', '05', '', 150
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1018/05' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000152, 'Kind_61a20f96312e419c', '小类', '1018/06', '06', '', 151
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1018/06' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000153, 'Kind_61a20f96312e419c', '小类', '1018/07', '07', '', 152
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1018/07' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000154, 'Kind_61a20f96312e419c', '小类', '1018/08', '08', '08', 153
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1018/08' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000155, 'Kind_61a20f96312e419c', '小类', '1018/09', '09', '09', 154
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1018/09' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000156, 'Kind_61a20f96312e419c', '小类', '1018/10', '10', '10', 155
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1018/10' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000157, 'Kind_61a20f96312e419c', '大类', '1019', '1019', '', 156
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1019' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000158, 'Kind_61a20f96312e419c', '小类', '1019/01', '01', '', 157
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1019/01' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000159, 'Kind_61a20f96312e419c', '小类', '1019/02', '02', '', 158
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1019/02' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000160, 'Kind_61a20f96312e419c', '小类', '1019/03', '03', '', 159
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1019/03' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000161, 'Kind_61a20f96312e419c', '小类', '1019/04', '04', '', 160
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1019/04' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000162, 'Kind_61a20f96312e419c', '小类', '1019/05', '05', '', 161
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1019/05' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000163, 'Kind_61a20f96312e419c', '小类', '1019/06', '06', '', 162
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1019/06' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000164, 'Kind_61a20f96312e419c', '小类', '1019/07', '07', '', 163
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1019/07' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000165, 'Kind_61a20f96312e419c', '小类', '1019/08', '08', '', 164
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1019/08' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000166, 'Kind_61a20f96312e419c', '小类', '1019/09', '09', '', 165
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1019/09' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000167, 'Kind_61a20f96312e419c', '小类', '1019/11', '11', '11', 166
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1019/11' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000168, 'Kind_61a20f96312e419c', '小类', '1019/12', '12', '12', 167
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1019/12' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000169, 'Kind_61a20f96312e419c', '小类', '1019/13', '13', '13', 168
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1019/13' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000170, 'Kind_61a20f96312e419c', '大类', '1020', '1020', '', 169
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1020' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000171, 'Kind_61a20f96312e419c', '小类', '1020/01', '01', '', 170
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1020/01' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000172, 'Kind_61a20f96312e419c', '小类', '1020/02', '02', '', 171
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1020/02' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000173, 'Kind_61a20f96312e419c', '小类', '1020/03', '03', '03', 172
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1020/03' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000174, 'Kind_61a20f96312e419c', '小类', '1020/04', '04', '04', 173
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1020/04' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000175, 'Kind_61a20f96312e419c', '小类', '1020/05', '05', '05', 174
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1020/05' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000176, 'Kind_61a20f96312e419c', '小类', '1020/06', '06', '06', 175
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1020/06' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000177, 'Kind_61a20f96312e419c', '小类', '1020/07', '07', '07', 176
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_61a20f96312e419c' AND value_path = '1020/07' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000178, 'Kind_05ca91aa096c43bf', '大类', '1101', '1101', '', 177
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_05ca91aa096c43bf' AND value_path = '1101' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000179, 'Kind_05ca91aa096c43bf', '小类', '1101/01', '01', '', 178
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_05ca91aa096c43bf' AND value_path = '1101/01' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000180, 'Kind_05ca91aa096c43bf', '小类', '1101/02', '02', '', 179
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_05ca91aa096c43bf' AND value_path = '1101/02' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000181, 'Kind_05ca91aa096c43bf', '小类', '1101/03', '03', '', 180
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_05ca91aa096c43bf' AND value_path = '1101/03' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000182, 'Kind_05ca91aa096c43bf', '小类', '1101/04', '04', '', 181
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_05ca91aa096c43bf' AND value_path = '1101/04' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000183, 'Kind_05ca91aa096c43bf', '小类', '1101/05', '05', '', 182
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_05ca91aa096c43bf' AND value_path = '1101/05' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000184, 'Kind_05ca91aa096c43bf', '大类', '1102', '1102', '', 183
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_05ca91aa096c43bf' AND value_path = '1102' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000185, 'Kind_05ca91aa096c43bf', '小类', '1102/01', '01', '', 184
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_05ca91aa096c43bf' AND value_path = '1102/01' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000186, 'Kind_05ca91aa096c43bf', '小类', '1102/02', '02', '', 185
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_05ca91aa096c43bf' AND value_path = '1102/02' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000187, 'Kind_05ca91aa096c43bf', '小类', '1102/03', '03', '', 186
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_05ca91aa096c43bf' AND value_path = '1102/03' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000188, 'Kind_05ca91aa096c43bf', '小类', '1102/04', '04', '', 187
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_05ca91aa096c43bf' AND value_path = '1102/04' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000189, 'Kind_05ca91aa096c43bf', '大类', '1103', '1103', '', 188
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_05ca91aa096c43bf' AND value_path = '1103' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000190, 'Kind_05ca91aa096c43bf', '小类', '1103/01', '01', '', 189
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_05ca91aa096c43bf' AND value_path = '1103/01' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000191, 'Kind_05ca91aa096c43bf', '小类', '1103/02', '02', '', 190
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_05ca91aa096c43bf' AND value_path = '1103/02' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000192, 'Kind_05ca91aa096c43bf', '小类', '1103/03', '03', '', 191
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_05ca91aa096c43bf' AND value_path = '1103/03' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000193, 'Kind_05ca91aa096c43bf', '大类', '1104', '1104', '', 192
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_05ca91aa096c43bf' AND value_path = '1104' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000194, 'Kind_05ca91aa096c43bf', '小类', '1104/01', '01', '', 193
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_05ca91aa096c43bf' AND value_path = '1104/01' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000195, 'Kind_05ca91aa096c43bf', '小类', '1104/02', '02', '', 194
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_05ca91aa096c43bf' AND value_path = '1104/02' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000196, 'Kind_05ca91aa096c43bf', '小类', '1104/03', '03', '', 195
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_05ca91aa096c43bf' AND value_path = '1104/03' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000197, 'Kind_05ca91aa096c43bf', '小类', '1104/04', '04', '', 196
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_05ca91aa096c43bf' AND value_path = '1104/04' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000198, 'Kind_05ca91aa096c43bf', '小类', '1104/05', '05', '', 197
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_05ca91aa096c43bf' AND value_path = '1104/05' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000199, 'Kind_05ca91aa096c43bf', '小类', '1104/06', '06', '', 198
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_05ca91aa096c43bf' AND value_path = '1104/06' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000200, 'Kind_05ca91aa096c43bf', '小类', '1104/07', '07', '', 199
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_05ca91aa096c43bf' AND value_path = '1104/07' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000201, 'Kind_05ca91aa096c43bf', '小类', '1104/08', '08', '08', 200
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_05ca91aa096c43bf' AND value_path = '1104/08' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000202, 'Kind_39f8cd293eb940ca', '大类', '1201', '1201', '', 201
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_39f8cd293eb940ca' AND value_path = '1201' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000203, 'Kind_39f8cd293eb940ca', '小类', '1201/02', '02', '02', 202
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_39f8cd293eb940ca' AND value_path = '1201/02' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000204, 'Kind_39f8cd293eb940ca', '小类', '1201/03', '03', '03', 203
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_39f8cd293eb940ca' AND value_path = '1201/03' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000205, 'Kind_39f8cd293eb940ca', '小类', '1201/04', '04', '04', 204
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_39f8cd293eb940ca' AND value_path = '1201/04' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000206, 'Kind_39f8cd293eb940ca', '小类', '1201/05', '05', '05', 205
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_39f8cd293eb940ca' AND value_path = '1201/05' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000207, 'Kind_39f8cd293eb940ca', '小类', '1201/06', '06', '06', 206
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_39f8cd293eb940ca' AND value_path = '1201/06' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000208, 'Kind_39f8cd293eb940ca', '小类', '1201/08', '08', '08', 207
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_39f8cd293eb940ca' AND value_path = '1201/08' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000209, 'Kind_39f8cd293eb940ca', '小类', '1201/09', '09', '09', 208
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_39f8cd293eb940ca' AND value_path = '1201/09' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000210, 'Kind_39f8cd293eb940ca', '小类', '1201/10', '10', '10', 209
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_39f8cd293eb940ca' AND value_path = '1201/10' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000211, 'Kind_39f8cd293eb940ca', '小类', '1201/11', '11', '11', 210
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_39f8cd293eb940ca' AND value_path = '1201/11' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000212, 'Kind_39f8cd293eb940ca', '大类', '1202', '1202', '', 211
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_39f8cd293eb940ca' AND value_path = '1202' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000213, 'Kind_39f8cd293eb940ca', '小类', '1202/01', '01', '', 212
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_39f8cd293eb940ca' AND value_path = '1202/01' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000214, 'Kind_39f8cd293eb940ca', '小类', '1202/02', '02', '', 213
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_39f8cd293eb940ca' AND value_path = '1202/02' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000215, 'Kind_39f8cd293eb940ca', '小类', '1202/03', '03', '', 214
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_39f8cd293eb940ca' AND value_path = '1202/03' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000216, 'Kind_39f8cd293eb940ca', '小类', '1202/04', '04', '', 215
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_39f8cd293eb940ca' AND value_path = '1202/04' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000217, 'Kind_39f8cd293eb940ca', '小类', '1202/05', '05', '', 216
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_39f8cd293eb940ca' AND value_path = '1202/05' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000218, 'Kind_39f8cd293eb940ca', '小类', '1202/06', '06', '06', 217
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_39f8cd293eb940ca' AND value_path = '1202/06' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000219, 'Kind_39f8cd293eb940ca', '小类', '1202/07', '07', '07', 218
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_39f8cd293eb940ca' AND value_path = '1202/07' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000220, 'Kind_39f8cd293eb940ca', '小类', '1202/08', '08', '08', 219
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_39f8cd293eb940ca' AND value_path = '1202/08' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000221, 'Kind_39f8cd293eb940ca', '大类', '1203', '1203', '1203', 220
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_39f8cd293eb940ca' AND value_path = '1203' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000222, 'Kind_39f8cd293eb940ca', '小类', '1203/01', '01', '01', 221
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_39f8cd293eb940ca' AND value_path = '1203/01' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000223, 'Kind_39f8cd293eb940ca', '小类', '1203/02', '02', '02', 222
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_39f8cd293eb940ca' AND value_path = '1203/02' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000224, 'Kind_39f8cd293eb940ca', '小类', '1203/03', '03', '03', 223
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_39f8cd293eb940ca' AND value_path = '1203/03' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000225, 'Kind_39f8cd293eb940ca', '小类', '1203/04', '04', '04', 224
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_39f8cd293eb940ca' AND value_path = '1203/04' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000226, 'Kind_39f8cd293eb940ca', '小类', '1203/05', '05', '05', 225
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_39f8cd293eb940ca' AND value_path = '1203/05' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000227, 'Kind_39f8cd293eb940ca', '小类', '1203/06', '06', '06', 226
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_39f8cd293eb940ca' AND value_path = '1203/06' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000228, 'Kind_39f8cd293eb940ca', '小类', '1203/07', '07', '07', 227
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_39f8cd293eb940ca' AND value_path = '1203/07' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000229, 'Kind_39f8cd293eb940ca', '大类', '1204', '1204', '1204', 228
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_39f8cd293eb940ca' AND value_path = '1204' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000230, 'Kind_39f8cd293eb940ca', '小类', '1204/01', '01', '01', 229
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_39f8cd293eb940ca' AND value_path = '1204/01' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000231, 'Kind_39f8cd293eb940ca', '小类', '1204/02', '02', '02', 230
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_39f8cd293eb940ca' AND value_path = '1204/02' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000232, 'Kind_39f8cd293eb940ca', '小类', '1204/03', '03', '03', 231
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_39f8cd293eb940ca' AND value_path = '1204/03' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000233, 'Kind_39f8cd293eb940ca', '小类', '1204/04', '04', '04', 232
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_39f8cd293eb940ca' AND value_path = '1204/04' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000234, 'Kind_39f8cd293eb940ca', '小类', '1204/05', '05', '05', 233
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_39f8cd293eb940ca' AND value_path = '1204/05' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000235, 'Kind_39f8cd293eb940ca', '小类', '1204/06', '06', '06', 234
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_39f8cd293eb940ca' AND value_path = '1204/06' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000236, 'Kind_39f8cd293eb940ca', '小类', '1204/07', '07', '07', 235
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_39f8cd293eb940ca' AND value_path = '1204/07' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000237, 'Kind_39f8cd293eb940ca', '小类', '1204/08', '08', '08', 236
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_39f8cd293eb940ca' AND value_path = '1204/08' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000238, 'Kind_e157a0b00f4742b2', '大类', '8010', '8010', '8010', 237
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8010' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000239, 'Kind_e157a0b00f4742b2', '小类', '8010/01', '01', '01', 238
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8010/01' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000240, 'Kind_e157a0b00f4742b2', '小类', '8010/02', '02', '02', 239
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8010/02' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000241, 'Kind_e157a0b00f4742b2', '小类', '8010/03', '03', '03', 240
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8010/03' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000242, 'Kind_e157a0b00f4742b2', '小类', '8010/04', '04', '04', 241
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8010/04' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000243, 'Kind_e157a0b00f4742b2', '小类', '8010/05', '05', '05', 242
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8010/05' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000244, 'Kind_e157a0b00f4742b2', '小类', '8010/06', '06', '06', 243
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8010/06' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000245, 'Kind_e157a0b00f4742b2', '小类', '8010/07', '07', '07', 244
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8010/07' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000246, 'Kind_e157a0b00f4742b2', '小类', '8010/08', '08', '08', 245
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8010/08' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000247, 'Kind_e157a0b00f4742b2', '小类', '8010/09', '09', '09', 246
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8010/09' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000248, 'Kind_e157a0b00f4742b2', '小类', '8010/10', '10', '10', 247
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8010/10' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000249, 'Kind_e157a0b00f4742b2', '小类', '8010/11', '11', '11', 248
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8010/11' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000250, 'Kind_e157a0b00f4742b2', '小类', '8010/12', '12', '12', 249
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8010/12' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000251, 'Kind_e157a0b00f4742b2', '小类', '8010/13', '13', '13', 250
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8010/13' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000252, 'Kind_e157a0b00f4742b2', '小类', '8010/14', '14', '14', 251
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8010/14' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000253, 'Kind_e157a0b00f4742b2', '小类', '8010/15', '15', '15', 252
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8010/15' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000254, 'Kind_e157a0b00f4742b2', '小类', '8010/16', '16', '16', 253
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8010/16' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000255, 'Kind_e157a0b00f4742b2', '小类', '8010/17', '17', '17', 254
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8010/17' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000256, 'Kind_e157a0b00f4742b2', '小类', '8010/18', '18', '18', 255
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8010/18' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000257, 'Kind_e157a0b00f4742b2', '小类', '8010/19', '19', '19', 256
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8010/19' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000258, 'Kind_e157a0b00f4742b2', '小类', '8010/20', '20', '20', 257
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8010/20' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000259, 'Kind_e157a0b00f4742b2', '小类', '8010/21', '21', '21', 258
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8010/21' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000260, 'Kind_e157a0b00f4742b2', '小类', '8010/22', '22', '22', 259
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8010/22' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000261, 'Kind_e157a0b00f4742b2', '小类', '8010/23', '23', '23', 260
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8010/23' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000262, 'Kind_e157a0b00f4742b2', '小类', '8010/24', '24', '24', 261
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8010/24' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000263, 'Kind_e157a0b00f4742b2', '小类', '8010/25', '25', '25', 262
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8010/25' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000264, 'Kind_e157a0b00f4742b2', '小类', '8010/26', '26', '26', 263
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8010/26' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000265, 'Kind_e157a0b00f4742b2', '小类', '8010/27', '27', '27', 264
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8010/27' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000266, 'Kind_e157a0b00f4742b2', '小类', '8010/28', '28', '28', 265
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8010/28' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000267, 'Kind_e157a0b00f4742b2', '小类', '8010/29', '29', '29', 266
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8010/29' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000268, 'Kind_e157a0b00f4742b2', '大类', '8011', '8011', '8011', 267
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8011' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000269, 'Kind_e157a0b00f4742b2', '小类', '8011/01', '01', '01', 268
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8011/01' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000270, 'Kind_e157a0b00f4742b2', '小类', '8011/02', '02', '02', 269
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8011/02' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000271, 'Kind_e157a0b00f4742b2', '小类', '8011/03', '03', '03', 270
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8011/03' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000272, 'Kind_e157a0b00f4742b2', '小类', '8011/04', '04', '04', 271
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8011/04' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000273, 'Kind_e157a0b00f4742b2', '大类', '8012', '8012', '8012', 272
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8012' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000274, 'Kind_e157a0b00f4742b2', '小类', '8012/01', '01', '01', 273
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8012/01' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000275, 'Kind_e157a0b00f4742b2', '小类', '8012/02', '02', '02', 274
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8012/02' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000276, 'Kind_e157a0b00f4742b2', '小类', '8012/03', '03', '03', 275
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8012/03' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000277, 'Kind_e157a0b00f4742b2', '大类', '8013', '8013', '8013', 276
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8013' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000278, 'Kind_e157a0b00f4742b2', '小类', '8013/01', '01', '01', 277
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8013/01' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000279, 'Kind_e157a0b00f4742b2', '小类', '8013/02', '02', '02', 278
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8013/02' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000280, 'Kind_e157a0b00f4742b2', '小类', '8013/03', '03', '03', 279
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8013/03' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000281, 'Kind_e157a0b00f4742b2', '小类', '8013/04', '04', '04', 280
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8013/04' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000282, 'Kind_e157a0b00f4742b2', '小类', '8013/05', '05', '05', 281
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8013/05' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000283, 'Kind_e157a0b00f4742b2', '大类', '8014', '8014', '8014', 282
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8014' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000284, 'Kind_e157a0b00f4742b2', '小类', '8014/01', '01', '01', 283
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8014/01' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000285, 'Kind_e157a0b00f4742b2', '小类', '8014/02', '02', '02', 284
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8014/02' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000286, 'Kind_e157a0b00f4742b2', '小类', '8014/03', '03', '03', 285
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8014/03' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000287, 'Kind_e157a0b00f4742b2', '小类', '8014/04', '04', '04', 286
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8014/04' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000288, 'Kind_e157a0b00f4742b2', '小类', '8014/05', '05', '05', 287
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8014/05' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000289, 'Kind_e157a0b00f4742b2', '大类', '8015', '8015', '8015', 288
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8015' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000290, 'Kind_e157a0b00f4742b2', '小类', '8015/01', '01', '01', 289
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8015/01' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000291, 'Kind_e157a0b00f4742b2', '小类', '8015/02', '02', '02', 290
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8015/02' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000292, 'Kind_e157a0b00f4742b2', '小类', '8015/03', '03', '03', 291
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8015/03' AND deleted = false);
INSERT INTO code_segment_value (id, kind_code, segment, value_path, code_value, name, sort)
SELECT 9500000000293, 'Kind_e157a0b00f4742b2', '小类', '8015/04', '04', '04', 292
 WHERE NOT EXISTS (SELECT 1 FROM code_segment_value
                    WHERE kind_code = 'Kind_e157a0b00f4742b2' AND value_path = '8015/04' AND deleted = false);

-- ---- 3. 分类绑定路径 ----
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1001/01/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100101' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1001/02/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100102' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1001/03/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100103' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1001/04/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100104' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1001/05/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100105' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1001/06/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100106' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1001/07/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100107' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1001/08/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100108' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1002/01/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100201' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1002/02/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100202' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1002/03/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100203' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1002/04/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100204' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1003/01/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100301' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1003/02/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100302' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1003/03/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100303' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1003/04/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100304' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1003/05/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100305' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1004/01/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100401' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1004/02/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100402' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1004/03/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100403' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1004/04/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100404' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1004/05/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100405' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1004/06/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100406' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1004/07/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100407' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1004/08/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100408' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1005/01/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100501' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1005/02/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100502' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1005/03/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100503' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1005/04/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100504' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1005/05/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100505' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1005/06/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100506' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1005/07/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100507' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1005/08/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100508' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1005/09/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100509' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1005/10/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100510' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1006/01/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100601' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1006/02/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100602' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1007/01/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100701' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1007/02/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100702' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1007/03/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100703' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1007/04/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100704' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1008/01/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100801' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1008/02/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100802' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1008/03/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100803' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1008/04/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100804' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1008/05/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100805' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1009/01/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100901' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1009/02/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100902' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1009/03/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100903' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1009/04/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100904' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1009/05/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100905' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1009/06/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100906' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1009/07/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100907' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1009/08/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100908' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1009/09/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100909' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1009/10/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100910' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1009/11/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100911' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1009/12/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100912' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1009/13/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100913' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1009/14/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '100914' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1010/01/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101001' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1010/02/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101002' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1010/03/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101003' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1010/04/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101004' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1010/05/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101005' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1010/06/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101006' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1010/07/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101007' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1010/08/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101008' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1010/09/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101009' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1010/10/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101010' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1011/01/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101101' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1011/02/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101102' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1011/03/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101103' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1012/01/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101201' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1012/02/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101202' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1012/03/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101203' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1012/04/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101204' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1012/05/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101205' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1012/06/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101206' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1013/01/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101301' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1013/02/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101302' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1013/03/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101303' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1013/04/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101304' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1013/05/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101305' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1013/06/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101306' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1013/07/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101307' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1013/08/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101308' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1014/01/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101401' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1014/02/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101402' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1014/03/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101403' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1014/04/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101404' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1014/05/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101405' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1014/06/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101406' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1014/07/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101407' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1014/08/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101408' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1014/09/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101409' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1014/10/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101410' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1014/11/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101411' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1015/01/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101501' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1015/02/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101502' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1015/03/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101503' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1015/04/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101504' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1015/05/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101505' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1015/06/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101506' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1015/07/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101507' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1015/08/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101508' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1015/09/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101509' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1015/10/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101510' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1015/11/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101511' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1015/12/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101512' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1015/13/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101513' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1015/14/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101514' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1015/15/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101515' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1015/16/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101516' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1016/01/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101601' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1016/02/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101602' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1016/03/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101603' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1016/04/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101604' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1016/05/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101605' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1016/06/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101606' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1016/07/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101607' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1017/01/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101701' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1017/02/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101702' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1017/03/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101703' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1017/04/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101704' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1017/05/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101705' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1017/06/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101706' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1018/01/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101801' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1018/02/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101802' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1018/03/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101803' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1018/04/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101804' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1018/05/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101805' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1018/06/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101806' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1018/07/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101807' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1018/08/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101808' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1018/09/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101809' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1018/10/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101810' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1019/01/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101901' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1019/02/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101902' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1019/03/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101903' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1019/04/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101904' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1019/05/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101905' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1019/06/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101906' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1019/07/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101907' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1019/08/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101908' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1019/09/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101909' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1019/11/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101911' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1019/12/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101912' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1019/13/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '101913' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1020/01/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '102001' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1020/02/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '102002' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1020/03/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '102003' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1020/04/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '102004' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1020/05/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '102005' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1020/06/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '102006' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_61a20f96312e419c/1020/07/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '102007' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_05ca91aa096c43bf/1101/01/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '110101' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_05ca91aa096c43bf/1101/02/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '110102' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_05ca91aa096c43bf/1101/03/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '110103' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_05ca91aa096c43bf/1101/04/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '110104' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_05ca91aa096c43bf/1101/05/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '110105' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_05ca91aa096c43bf/1102/01/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '110201' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_05ca91aa096c43bf/1102/02/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '110202' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_05ca91aa096c43bf/1102/03/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '110203' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_05ca91aa096c43bf/1102/04/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '110204' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_05ca91aa096c43bf/1103/01/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '110301' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_05ca91aa096c43bf/1103/02/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '110302' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_05ca91aa096c43bf/1103/03/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '110303' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_05ca91aa096c43bf/1104/01/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '110401' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_05ca91aa096c43bf/1104/02/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '110402' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_05ca91aa096c43bf/1104/03/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '110403' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_05ca91aa096c43bf/1104/04/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '110404' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_05ca91aa096c43bf/1104/05/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '110405' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_05ca91aa096c43bf/1104/06/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '110406' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_05ca91aa096c43bf/1104/07/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '110407' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_05ca91aa096c43bf/1104/08/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '110408' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_39f8cd293eb940ca/1201/02/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '120102' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_39f8cd293eb940ca/1201/03/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '120103' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_39f8cd293eb940ca/1201/04/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '120104' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_39f8cd293eb940ca/1201/05/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '120105' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_39f8cd293eb940ca/1201/06/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '120106' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_39f8cd293eb940ca/1201/08/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '120108' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_39f8cd293eb940ca/1201/09/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '120109' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_39f8cd293eb940ca/1201/10/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '120110' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_39f8cd293eb940ca/1201/11/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '120111' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_39f8cd293eb940ca/1202/01/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '120201' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_39f8cd293eb940ca/1202/02/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '120202' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_39f8cd293eb940ca/1202/03/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '120203' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_39f8cd293eb940ca/1202/04/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '120204' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_39f8cd293eb940ca/1202/05/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '120205' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_39f8cd293eb940ca/1202/06/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '120206' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_39f8cd293eb940ca/1202/07/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '120207' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_39f8cd293eb940ca/1202/08/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '120208' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_39f8cd293eb940ca/1203/01/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '120301' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_39f8cd293eb940ca/1203/02/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '120302' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_39f8cd293eb940ca/1203/03/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '120303' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_39f8cd293eb940ca/1203/04/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '120304' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_39f8cd293eb940ca/1203/05/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '120305' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_39f8cd293eb940ca/1203/06/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '120306' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_39f8cd293eb940ca/1203/07/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '120307' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_39f8cd293eb940ca/1204/01/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '120401' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_39f8cd293eb940ca/1204/02/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '120402' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_39f8cd293eb940ca/1204/03/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '120403' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_39f8cd293eb940ca/1204/04/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '120404' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_39f8cd293eb940ca/1204/05/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '120405' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_39f8cd293eb940ca/1204/06/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '120406' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_39f8cd293eb940ca/1204/07/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '120407' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_39f8cd293eb940ca/1204/08/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '120408' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8010/01/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801001' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8010/02/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801002' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8010/03/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801003' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8010/04/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801004' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8010/05/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801005' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8010/06/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801006' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8010/07/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801007' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8010/08/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801008' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8010/09/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801009' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8010/10/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801010' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8010/11/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801011' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8010/12/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801012' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8010/13/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801013' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8010/14/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801014' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8010/15/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801015' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8010/16/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801016' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8010/17/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801017' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8010/18/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801018' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8010/19/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801019' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8010/20/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801020' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8010/21/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801021' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8010/22/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801022' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8010/23/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801023' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8010/24/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801024' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8010/25/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801025' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8010/26/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801026' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8010/27/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801027' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8010/28/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801028' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8010/29/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801029' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8011/01/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801101' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8011/02/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801102' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8011/03/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801103' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8011/04/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801104' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8012/01/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801201' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8012/02/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801202' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8012/03/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801203' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8013/01/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801301' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8013/02/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801302' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8013/03/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801303' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8013/04/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801304' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8013/05/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801305' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8014/01/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801401' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8014/02/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801402' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8014/03/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801403' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8014/04/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801404' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8014/05/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801405' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8015/01/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801501' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8015/02/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801502' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8015/03/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801503' AND deleted = false;
UPDATE doc_class SET code_path = 'Kind_e157a0b00f4742b2/8015/04/*', updated_at = now()
 WHERE entity_code = 'PdmItem' AND code = '801504' AND deleted = false;

COMMIT;
