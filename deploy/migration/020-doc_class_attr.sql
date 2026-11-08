-- =====================================================================
-- 分类属性迁移：蓝本 CLASSIFYATTRIBUTE + CLASSIFYRELATION → 新系统 doc_class_attr
-- 生成器：tools/Gen-DocClassAttrMigration.ps1（只读蓝本，仅生成 SQL，不执行）
-- 数据源：198.168.6.26,1433/TCPLM；生成时间：2026-11-08 15:09:28
-- 统计：属性关系 1287 条 → 生成 1287 条；跳过 0 条
-- 口径：
--   1) 属性编码 = ATTR + 蓝本编码（蓝本是纯数字 000001，新系统要求字母开头）
--   2) value_type 一律 STRING（蓝本 DATATYPE 全为 String200）
--   3) required 一律 false + inheritable true（ISEMPTY 方向未取证，见文件末尾待确认清单）
--   4) class_id 取自 010-doc_class.sql 的 id 映射，保证两份文件一致
-- 注意：执行前需先执行 010（分类树），并确认目标库写权限与业务窗口。
-- =====================================================================

BEGIN;

INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000001, 9000000000098, 'ATTR000024', '电流方式', 'STRING', NULL, false, true, NULL, 24, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000098 AND code = 'ATTR000024');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000002, 9000000000098, 'ATTR000035', '额定电流', 'STRING', NULL, false, true, NULL, 35, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000098 AND code = 'ATTR000035');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000003, 9000000000098, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000098 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000004, 9000000000098, 'ATTR000059', '极数', 'STRING', NULL, false, true, NULL, 59, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000098 AND code = 'ATTR000059');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000005, 9000000000098, 'ATTR000070', '框架电流', 'STRING', NULL, false, true, NULL, 70, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000098 AND code = 'ATTR000070');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000006, 9000000000098, 'ATTR000075', '频率', 'STRING', NULL, false, true, NULL, 75, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000098 AND code = 'ATTR000075');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000007, 9000000000098, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000098 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000008, 9000000000098, 'ATTR000089', '脱扣器类型', 'STRING', NULL, false, true, NULL, 89, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000098 AND code = 'ATTR000089');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000009, 9000000000098, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000098 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000010, 9000000000098, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000098 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000011, 9000000000098, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000098 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000012, 9000000000099, 'ATTR000024', '电流方式', 'STRING', NULL, false, true, NULL, 24, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000099 AND code = 'ATTR000024');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000013, 9000000000099, 'ATTR000035', '额定电流', 'STRING', NULL, false, true, NULL, 35, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000099 AND code = 'ATTR000035');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000014, 9000000000099, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000099 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000015, 9000000000099, 'ATTR000059', '极数', 'STRING', NULL, false, true, NULL, 59, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000099 AND code = 'ATTR000059');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000016, 9000000000099, 'ATTR000070', '框架电流', 'STRING', NULL, false, true, NULL, 70, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000099 AND code = 'ATTR000070');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000017, 9000000000099, 'ATTR000075', '频率', 'STRING', NULL, false, true, NULL, 75, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000099 AND code = 'ATTR000075');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000018, 9000000000099, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000099 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000019, 9000000000099, 'ATTR000089', '脱扣器类型', 'STRING', NULL, false, true, NULL, 89, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000099 AND code = 'ATTR000089');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000020, 9000000000099, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000099 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000021, 9000000000099, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000099 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000022, 9000000000099, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000099 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000023, 9000000000100, 'ATTR000002', '保护用途', 'STRING', NULL, false, true, NULL, 2, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000100 AND code = 'ATTR000002');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000024, 9000000000100, 'ATTR000013', '操作方式', 'STRING', NULL, false, true, NULL, 13, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000100 AND code = 'ATTR000013');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000025, 9000000000100, 'ATTR000035', '额定电流', 'STRING', NULL, false, true, NULL, 35, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000100 AND code = 'ATTR000035');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000026, 9000000000100, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000100 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000027, 9000000000100, 'ATTR000045', '分断能力', 'STRING', NULL, false, true, NULL, 45, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000100 AND code = 'ATTR000045');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000028, 9000000000100, 'ATTR000059', '极数', 'STRING', NULL, false, true, NULL, 59, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000100 AND code = 'ATTR000059');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000029, 9000000000100, 'ATTR000061', '接线形式', 'STRING', NULL, false, true, NULL, 61, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000100 AND code = 'ATTR000061');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000030, 9000000000100, 'ATTR000070', '框架电流', 'STRING', NULL, false, true, NULL, 70, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000100 AND code = 'ATTR000070');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000031, 9000000000100, 'ATTR000075', '频率', 'STRING', NULL, false, true, NULL, 75, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000100 AND code = 'ATTR000075');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000032, 9000000000100, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000100 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000033, 9000000000100, 'ATTR000089', '脱扣器类型', 'STRING', NULL, false, true, NULL, 89, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000100 AND code = 'ATTR000089');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000034, 9000000000100, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000100 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000035, 9000000000100, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000100 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000036, 9000000000100, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000100 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000037, 9000000000101, 'ATTR000002', '保护用途', 'STRING', NULL, false, true, NULL, 2, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000101 AND code = 'ATTR000002');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000038, 9000000000101, 'ATTR000013', '操作方式', 'STRING', NULL, false, true, NULL, 13, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000101 AND code = 'ATTR000013');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000039, 9000000000101, 'ATTR000035', '额定电流', 'STRING', NULL, false, true, NULL, 35, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000101 AND code = 'ATTR000035');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000040, 9000000000101, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000101 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000041, 9000000000101, 'ATTR000045', '分断能力', 'STRING', NULL, false, true, NULL, 45, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000101 AND code = 'ATTR000045');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000042, 9000000000101, 'ATTR000059', '极数', 'STRING', NULL, false, true, NULL, 59, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000101 AND code = 'ATTR000059');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000043, 9000000000101, 'ATTR000061', '接线形式', 'STRING', NULL, false, true, NULL, 61, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000101 AND code = 'ATTR000061');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000044, 9000000000101, 'ATTR000070', '框架电流', 'STRING', NULL, false, true, NULL, 70, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000101 AND code = 'ATTR000070');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000045, 9000000000101, 'ATTR000075', '频率', 'STRING', NULL, false, true, NULL, 75, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000101 AND code = 'ATTR000075');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000046, 9000000000101, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000101 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000047, 9000000000101, 'ATTR000089', '脱扣器类型', 'STRING', NULL, false, true, NULL, 89, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000101 AND code = 'ATTR000089');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000048, 9000000000101, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000101 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000049, 9000000000101, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000101 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000050, 9000000000101, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000101 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000051, 9000000000102, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000102 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000052, 9000000000102, 'ATTR000045', '分断能力', 'STRING', NULL, false, true, NULL, 45, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000102 AND code = 'ATTR000045');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000053, 9000000000102, 'ATTR000059', '极数', 'STRING', NULL, false, true, NULL, 59, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000102 AND code = 'ATTR000059');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000054, 9000000000102, 'ATTR000070', '框架电流', 'STRING', NULL, false, true, NULL, 70, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000102 AND code = 'ATTR000070');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000055, 9000000000102, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000102 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000056, 9000000000102, 'ATTR000090', '脱扣特性', 'STRING', NULL, false, true, NULL, 90, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000102 AND code = 'ATTR000090');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000057, 9000000000102, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000102 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000058, 9000000000102, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000102 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000059, 9000000000102, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000102 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000060, 9000000000103, 'ATTR000002', '保护用途', 'STRING', NULL, false, true, NULL, 2, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000103 AND code = 'ATTR000002');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000061, 9000000000103, 'ATTR000013', '操作方式', 'STRING', NULL, false, true, NULL, 13, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000103 AND code = 'ATTR000013');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000062, 9000000000103, 'ATTR000035', '额定电流', 'STRING', NULL, false, true, NULL, 35, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000103 AND code = 'ATTR000035');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000063, 9000000000103, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000103 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000064, 9000000000103, 'ATTR000045', '分断能力', 'STRING', NULL, false, true, NULL, 45, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000103 AND code = 'ATTR000045');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000065, 9000000000103, 'ATTR000059', '极数', 'STRING', NULL, false, true, NULL, 59, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000103 AND code = 'ATTR000059');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000066, 9000000000103, 'ATTR000061', '接线形式', 'STRING', NULL, false, true, NULL, 61, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000103 AND code = 'ATTR000061');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000067, 9000000000103, 'ATTR000070', '框架电流', 'STRING', NULL, false, true, NULL, 70, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000103 AND code = 'ATTR000070');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000068, 9000000000103, 'ATTR000075', '频率', 'STRING', NULL, false, true, NULL, 75, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000103 AND code = 'ATTR000075');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000069, 9000000000103, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000103 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000070, 9000000000103, 'ATTR000089', '脱扣器类型', 'STRING', NULL, false, true, NULL, 89, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000103 AND code = 'ATTR000089');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000071, 9000000000103, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000103 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000072, 9000000000103, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000103 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000073, 9000000000103, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000103 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000074, 9000000000103, 'ATTR000114', '剩余电流', 'STRING', NULL, false, true, NULL, 114, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000103 AND code = 'ATTR000114');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000075, 9000000000104, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000104 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000076, 9000000000104, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000104 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000077, 9000000000104, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000104 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000078, 9000000000104, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000104 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000079, 9000000000104, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000104 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000080, 9000000000105, 'ATTR000001', '安装方式', 'STRING', NULL, false, true, NULL, 1, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000105 AND code = 'ATTR000001');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000081, 9000000000105, 'ATTR000035', '额定电流', 'STRING', NULL, false, true, NULL, 35, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000105 AND code = 'ATTR000035');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000082, 9000000000105, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000105 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000083, 9000000000105, 'ATTR000037', '额定短路开断电流', 'STRING', NULL, false, true, NULL, 37, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000105 AND code = 'ATTR000037');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000084, 9000000000105, 'ATTR000058', '极间距', 'STRING', NULL, false, true, NULL, 58, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000105 AND code = 'ATTR000058');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000085, 9000000000105, 'ATTR000068', '控制电压', 'STRING', NULL, false, true, NULL, 68, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000105 AND code = 'ATTR000068');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000086, 9000000000105, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000105 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000087, 9000000000105, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000105 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000088, 9000000000105, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000105 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000089, 9000000000105, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000105 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000090, 9000000000106, 'ATTR000035', '额定电流', 'STRING', NULL, false, true, NULL, 35, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000106 AND code = 'ATTR000035');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000091, 9000000000106, 'ATTR000075', '频率', 'STRING', NULL, false, true, NULL, 75, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000106 AND code = 'ATTR000075');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000092, 9000000000106, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000106 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000093, 9000000000106, 'ATTR000088', '特殊品种', 'STRING', NULL, false, true, NULL, 88, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000106 AND code = 'ATTR000088');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000094, 9000000000106, 'ATTR000094', '线圈电压', 'STRING', NULL, false, true, NULL, 94, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000106 AND code = 'ATTR000094');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000095, 9000000000106, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000106 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000096, 9000000000106, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000106 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000097, 9000000000106, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000106 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000098, 9000000000107, 'ATTR000035', '额定电流', 'STRING', NULL, false, true, NULL, 35, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000107 AND code = 'ATTR000035');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000099, 9000000000107, 'ATTR000075', '频率', 'STRING', NULL, false, true, NULL, 75, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000107 AND code = 'ATTR000075');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000100, 9000000000107, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000107 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000101, 9000000000107, 'ATTR000088', '特殊品种', 'STRING', NULL, false, true, NULL, 88, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000107 AND code = 'ATTR000088');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000102, 9000000000107, 'ATTR000094', '线圈电压', 'STRING', NULL, false, true, NULL, 94, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000107 AND code = 'ATTR000094');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000103, 9000000000107, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000107 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000104, 9000000000107, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000107 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000105, 9000000000107, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000107 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000106, 9000000000108, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000108 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000107, 9000000000108, 'ATTR000094', '线圈电压', 'STRING', NULL, false, true, NULL, 94, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000108 AND code = 'ATTR000094');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000108, 9000000000108, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000108 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000109, 9000000000108, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000108 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000110, 9000000000108, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000108 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000111, 9000000000109, 'ATTR000001', '安装方式', 'STRING', NULL, false, true, NULL, 1, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000109 AND code = 'ATTR000001');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000112, 9000000000109, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000109 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000113, 9000000000109, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000109 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000114, 9000000000109, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000109 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000115, 9000000000109, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000109 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000116, 9000000000110, 'ATTR000038', '额定工作电压', 'STRING', NULL, false, true, NULL, 38, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000110 AND code = 'ATTR000038');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000117, 9000000000110, 'ATTR000049', '辅助触头数量', 'STRING', NULL, false, true, NULL, 49, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000110 AND code = 'ATTR000049');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000118, 9000000000110, 'ATTR000059', '极数', 'STRING', NULL, false, true, NULL, 59, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000110 AND code = 'ATTR000059');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000119, 9000000000110, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000110 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000120, 9000000000110, 'ATTR000079', '熔断体形式', 'STRING', NULL, false, true, NULL, 79, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000110 AND code = 'ATTR000079');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000121, 9000000000110, 'ATTR000102', '约定发热电流', 'STRING', NULL, false, true, NULL, 102, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000110 AND code = 'ATTR000102');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000122, 9000000000110, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000110 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000123, 9000000000110, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000110 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000124, 9000000000110, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000110 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000125, 9000000000111, 'ATTR000038', '额定工作电压', 'STRING', NULL, false, true, NULL, 38, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000111 AND code = 'ATTR000038');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000126, 9000000000111, 'ATTR000049', '辅助触头数量', 'STRING', NULL, false, true, NULL, 49, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000111 AND code = 'ATTR000049');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000127, 9000000000111, 'ATTR000059', '极数', 'STRING', NULL, false, true, NULL, 59, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000111 AND code = 'ATTR000059');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000128, 9000000000111, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000111 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000129, 9000000000111, 'ATTR000079', '熔断体形式', 'STRING', NULL, false, true, NULL, 79, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000111 AND code = 'ATTR000079');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000130, 9000000000111, 'ATTR000102', '约定发热电流', 'STRING', NULL, false, true, NULL, 102, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000111 AND code = 'ATTR000102');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000131, 9000000000111, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000111 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000132, 9000000000111, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000111 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000133, 9000000000111, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000111 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000134, 9000000000112, 'ATTR000013', '操作方式', 'STRING', NULL, false, true, NULL, 13, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000112 AND code = 'ATTR000013');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000135, 9000000000112, 'ATTR000059', '极数', 'STRING', NULL, false, true, NULL, 59, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000112 AND code = 'ATTR000059');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000136, 9000000000112, 'ATTR000066', '绝缘板类型', 'STRING', NULL, false, true, NULL, 66, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000112 AND code = 'ATTR000066');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000137, 9000000000112, 'ATTR000074', '灭弧装置', 'STRING', NULL, false, true, NULL, 74, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000112 AND code = 'ATTR000074');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000138, 9000000000112, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000112 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000139, 9000000000112, 'ATTR000102', '约定发热电流', 'STRING', NULL, false, true, NULL, 102, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000112 AND code = 'ATTR000102');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000140, 9000000000112, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000112 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000141, 9000000000112, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000112 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000142, 9000000000112, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000112 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000143, 9000000000113, 'ATTR000013', '操作方式', 'STRING', NULL, false, true, NULL, 13, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000113 AND code = 'ATTR000013');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000144, 9000000000113, 'ATTR000059', '极数', 'STRING', NULL, false, true, NULL, 59, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000113 AND code = 'ATTR000059');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000145, 9000000000113, 'ATTR000066', '绝缘板类型', 'STRING', NULL, false, true, NULL, 66, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000113 AND code = 'ATTR000066');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000146, 9000000000113, 'ATTR000074', '灭弧装置', 'STRING', NULL, false, true, NULL, 74, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000113 AND code = 'ATTR000074');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000147, 9000000000113, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000113 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000148, 9000000000113, 'ATTR000102', '约定发热电流', 'STRING', NULL, false, true, NULL, 102, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000113 AND code = 'ATTR000102');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000149, 9000000000113, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000113 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000150, 9000000000113, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000113 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000151, 9000000000113, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000113 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000152, 9000000000114, 'ATTR000013', '操作方式', 'STRING', NULL, false, true, NULL, 13, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000114 AND code = 'ATTR000013');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000153, 9000000000114, 'ATTR000044', '防护形式', 'STRING', NULL, false, true, NULL, 44, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000114 AND code = 'ATTR000044');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000154, 9000000000114, 'ATTR000059', '极数', 'STRING', NULL, false, true, NULL, 59, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000114 AND code = 'ATTR000059');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000155, 9000000000114, 'ATTR000066', '绝缘板类型', 'STRING', NULL, false, true, NULL, 66, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000114 AND code = 'ATTR000066');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000156, 9000000000114, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000114 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000157, 9000000000114, 'ATTR000102', '约定发热电流', 'STRING', NULL, false, true, NULL, 102, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000114 AND code = 'ATTR000102');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000158, 9000000000114, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000114 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000159, 9000000000114, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000114 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000160, 9000000000114, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000114 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000161, 9000000000115, 'ATTR000017', '尺寸', 'STRING', NULL, false, true, NULL, 17, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000115 AND code = 'ATTR000017');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000162, 9000000000115, 'ATTR000035', '额定电流', 'STRING', NULL, false, true, NULL, 35, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000115 AND code = 'ATTR000035');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000163, 9000000000115, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000115 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000164, 9000000000115, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000115 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000165, 9000000000115, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000115 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000166, 9000000000115, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000115 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000167, 9000000000115, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000115 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000168, 9000000000116, 'ATTR000017', '尺寸', 'STRING', NULL, false, true, NULL, 17, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000116 AND code = 'ATTR000017');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000169, 9000000000116, 'ATTR000035', '额定电流', 'STRING', NULL, false, true, NULL, 35, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000116 AND code = 'ATTR000035');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000170, 9000000000116, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000116 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000171, 9000000000116, 'ATTR000059', '极数', 'STRING', NULL, false, true, NULL, 59, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000116 AND code = 'ATTR000059');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000172, 9000000000116, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000116 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000173, 9000000000116, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000116 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000174, 9000000000116, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000116 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000175, 9000000000116, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000116 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000176, 9000000000117, 'ATTR000035', '额定电流', 'STRING', NULL, false, true, NULL, 35, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000117 AND code = 'ATTR000035');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000177, 9000000000117, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000117 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000178, 9000000000117, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000117 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000179, 9000000000117, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000117 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000180, 9000000000117, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000117 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000181, 9000000000117, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000117 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000182, 9000000000118, 'ATTR000006', '标称放电电流', 'STRING', NULL, false, true, NULL, 6, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000118 AND code = 'ATTR000006');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000183, 9000000000118, 'ATTR000007', '标称放电电流最大残压', 'STRING', NULL, false, true, NULL, 7, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000118 AND code = 'ATTR000007');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000184, 9000000000118, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000118 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000185, 9000000000118, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000118 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000186, 9000000000118, 'ATTR000080', '使用场所', 'STRING', NULL, false, true, NULL, 80, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000118 AND code = 'ATTR000080');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000187, 9000000000118, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000118 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000188, 9000000000118, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000118 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000189, 9000000000118, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000118 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000190, 9000000000119, 'ATTR000006', '标称放电电流', 'STRING', NULL, false, true, NULL, 6, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000119 AND code = 'ATTR000006');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000191, 9000000000119, 'ATTR000059', '极数', 'STRING', NULL, false, true, NULL, 59, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000119 AND code = 'ATTR000059');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000192, 9000000000119, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000119 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000193, 9000000000119, 'ATTR000108', '最大持续工作电压', 'STRING', NULL, false, true, NULL, 108, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000119 AND code = 'ATTR000108');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000194, 9000000000119, 'ATTR000109', '最大冲击电流', 'STRING', NULL, false, true, NULL, 109, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000119 AND code = 'ATTR000109');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000195, 9000000000119, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000119 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000196, 9000000000119, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000119 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000197, 9000000000119, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000119 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000198, 9000000000120, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000120 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000199, 9000000000120, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000120 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000200, 9000000000120, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000120 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000201, 9000000000120, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000120 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000202, 9000000000121, 'ATTR000028', '电压等级', 'STRING', NULL, false, true, NULL, 28, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000121 AND code = 'ATTR000028');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000203, 9000000000121, 'ATTR000055', '功率', 'STRING', NULL, false, true, NULL, 55, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000121 AND code = 'ATTR000055');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000204, 9000000000121, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000121 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000205, 9000000000121, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000121 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000206, 9000000000121, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000121 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000207, 9000000000121, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000121 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000208, 9000000000122, 'ATTR000035', '额定电流', 'STRING', NULL, false, true, NULL, 35, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000122 AND code = 'ATTR000035');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000209, 9000000000122, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000122 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000210, 9000000000122, 'ATTR000087', '特定参数', 'STRING', NULL, false, true, NULL, 87, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000122 AND code = 'ATTR000087');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000211, 9000000000122, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000122 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000212, 9000000000122, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000122 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000213, 9000000000122, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000122 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000214, 9000000000123, 'ATTR000004', '变比', 'STRING', NULL, false, true, NULL, 4, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000123 AND code = 'ATTR000004');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000215, 9000000000123, 'ATTR000028', '电压等级', 'STRING', NULL, false, true, NULL, 28, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000123 AND code = 'ATTR000028');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000216, 9000000000123, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000123 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000217, 9000000000123, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000123 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000218, 9000000000123, 'ATTR000077', '容量', 'STRING', NULL, false, true, NULL, 77, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000123 AND code = 'ATTR000077');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000219, 9000000000123, 'ATTR000105', '准确级', 'STRING', NULL, false, true, NULL, 105, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000123 AND code = 'ATTR000105');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000220, 9000000000123, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000123 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000221, 9000000000123, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000123 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000222, 9000000000123, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000123 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000223, 9000000000124, 'ATTR000004', '变比', 'STRING', NULL, false, true, NULL, 4, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000124 AND code = 'ATTR000004');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000224, 9000000000124, 'ATTR000028', '电压等级', 'STRING', NULL, false, true, NULL, 28, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000124 AND code = 'ATTR000028');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000225, 9000000000124, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000124 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000226, 9000000000124, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000124 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000227, 9000000000124, 'ATTR000077', '容量', 'STRING', NULL, false, true, NULL, 77, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000124 AND code = 'ATTR000077');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000228, 9000000000124, 'ATTR000105', '准确级', 'STRING', NULL, false, true, NULL, 105, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000124 AND code = 'ATTR000105');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000229, 9000000000124, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000124 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000230, 9000000000124, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000124 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000231, 9000000000124, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000124 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000232, 9000000000125, 'ATTR000004', '变比', 'STRING', NULL, false, true, NULL, 4, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000125 AND code = 'ATTR000004');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000233, 9000000000125, 'ATTR000028', '电压等级', 'STRING', NULL, false, true, NULL, 28, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000125 AND code = 'ATTR000028');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000234, 9000000000125, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000125 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000235, 9000000000125, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000125 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000236, 9000000000125, 'ATTR000077', '容量', 'STRING', NULL, false, true, NULL, 77, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000125 AND code = 'ATTR000077');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000237, 9000000000125, 'ATTR000105', '准确级', 'STRING', NULL, false, true, NULL, 105, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000125 AND code = 'ATTR000105');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000238, 9000000000125, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000125 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000239, 9000000000125, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000125 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000240, 9000000000125, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000125 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000241, 9000000000126, 'ATTR000022', '电流变比', 'STRING', NULL, false, true, NULL, 22, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000126 AND code = 'ATTR000022');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000242, 9000000000126, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000126 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000243, 9000000000126, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000126 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000244, 9000000000126, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000126 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000245, 9000000000126, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000126 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000246, 9000000000127, 'ATTR000022', '电流变比', 'STRING', NULL, false, true, NULL, 22, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000127 AND code = 'ATTR000022');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000247, 9000000000127, 'ATTR000056', '供电电源', 'STRING', NULL, false, true, NULL, 56, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000127 AND code = 'ATTR000056');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000248, 9000000000127, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000127 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000249, 9000000000127, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000127 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000250, 9000000000127, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000127 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000251, 9000000000127, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000127 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000252, 9000000000128, 'ATTR000001', '安装方式', 'STRING', NULL, false, true, NULL, 1, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000128 AND code = 'ATTR000001');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000253, 9000000000128, 'ATTR000010', '材料', 'STRING', NULL, false, true, NULL, 10, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000128 AND code = 'ATTR000010');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000254, 9000000000128, 'ATTR000061', '接线形式', 'STRING', NULL, false, true, NULL, 61, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000128 AND code = 'ATTR000061');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000255, 9000000000128, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000128 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000256, 9000000000128, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000128 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000257, 9000000000128, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000128 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000258, 9000000000128, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000128 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000259, 9000000000129, 'ATTR000022', '电流变比', 'STRING', NULL, false, true, NULL, 22, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000129 AND code = 'ATTR000022');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000260, 9000000000129, 'ATTR000056', '供电电源', 'STRING', NULL, false, true, NULL, 56, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000129 AND code = 'ATTR000056');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000261, 9000000000129, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000129 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000262, 9000000000129, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000129 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000263, 9000000000129, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000129 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000264, 9000000000129, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000129 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000265, 9000000000130, 'ATTR000022', '电流变比', 'STRING', NULL, false, true, NULL, 22, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000130 AND code = 'ATTR000022');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000266, 9000000000130, 'ATTR000056', '供电电源', 'STRING', NULL, false, true, NULL, 56, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000130 AND code = 'ATTR000056');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000267, 9000000000130, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000130 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000268, 9000000000130, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000130 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000269, 9000000000130, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000130 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000270, 9000000000130, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000130 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000271, 9000000000131, 'ATTR000016', '测量介质', 'STRING', NULL, false, true, NULL, 16, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000131 AND code = 'ATTR000016');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000272, 9000000000131, 'ATTR000057', '供电方式', 'STRING', NULL, false, true, NULL, 57, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000131 AND code = 'ATTR000057');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000273, 9000000000131, 'ATTR000063', '结构', 'STRING', NULL, false, true, NULL, 63, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000131 AND code = 'ATTR000063');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000274, 9000000000131, 'ATTR000072', '量程范围', 'STRING', NULL, false, true, NULL, 72, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000131 AND code = 'ATTR000072');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000275, 9000000000131, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000131 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000276, 9000000000131, 'ATTR000084', '输出类型', 'STRING', NULL, false, true, NULL, 84, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000131 AND code = 'ATTR000084');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000277, 9000000000131, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000131 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000278, 9000000000131, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000131 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000279, 9000000000131, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000131 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000280, 9000000000132, 'ATTR000016', '测量介质', 'STRING', NULL, false, true, NULL, 16, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000132 AND code = 'ATTR000016');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000281, 9000000000132, 'ATTR000057', '供电方式', 'STRING', NULL, false, true, NULL, 57, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000132 AND code = 'ATTR000057');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000282, 9000000000132, 'ATTR000063', '结构', 'STRING', NULL, false, true, NULL, 63, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000132 AND code = 'ATTR000063');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000283, 9000000000132, 'ATTR000072', '量程范围', 'STRING', NULL, false, true, NULL, 72, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000132 AND code = 'ATTR000072');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000284, 9000000000132, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000132 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000285, 9000000000132, 'ATTR000084', '输出类型', 'STRING', NULL, false, true, NULL, 84, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000132 AND code = 'ATTR000084');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000286, 9000000000132, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000132 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000287, 9000000000132, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000132 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000288, 9000000000132, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000132 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000289, 9000000000133, 'ATTR000020', '电感值', 'STRING', NULL, false, true, NULL, 20, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000133 AND code = 'ATTR000020');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000290, 9000000000133, 'ATTR000021', '电抗率', 'STRING', NULL, false, true, NULL, 21, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000133 AND code = 'ATTR000021');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000291, 9000000000133, 'ATTR000035', '额定电流', 'STRING', NULL, false, true, NULL, 35, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000133 AND code = 'ATTR000035');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000292, 9000000000133, 'ATTR000053', '工作频率', 'STRING', NULL, false, true, NULL, 53, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000133 AND code = 'ATTR000053');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000293, 9000000000133, 'ATTR000071', '冷却方式', 'STRING', NULL, false, true, NULL, 71, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000133 AND code = 'ATTR000071');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000294, 9000000000133, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000133 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000295, 9000000000133, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000133 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000296, 9000000000133, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000133 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000297, 9000000000133, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000133 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000298, 9000000000134, 'ATTR000020', '电感值', 'STRING', NULL, false, true, NULL, 20, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000134 AND code = 'ATTR000020');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000299, 9000000000134, 'ATTR000021', '电抗率', 'STRING', NULL, false, true, NULL, 21, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000134 AND code = 'ATTR000021');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000300, 9000000000134, 'ATTR000035', '额定电流', 'STRING', NULL, false, true, NULL, 35, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000134 AND code = 'ATTR000035');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000301, 9000000000134, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000134 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000302, 9000000000134, 'ATTR000053', '工作频率', 'STRING', NULL, false, true, NULL, 53, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000134 AND code = 'ATTR000053');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000303, 9000000000134, 'ATTR000071', '冷却方式', 'STRING', NULL, false, true, NULL, 71, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000134 AND code = 'ATTR000071');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000304, 9000000000134, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000134 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000305, 9000000000134, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000134 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000306, 9000000000134, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000134 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000307, 9000000000134, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000134 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000308, 9000000000135, 'ATTR000011', '材质', 'STRING', NULL, false, true, NULL, 11, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000135 AND code = 'ATTR000011');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000309, 9000000000135, 'ATTR000027', '电压变比', 'STRING', NULL, false, true, NULL, 27, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000135 AND code = 'ATTR000027');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000310, 9000000000135, 'ATTR000071', '冷却方式', 'STRING', NULL, false, true, NULL, 71, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000135 AND code = 'ATTR000071');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000311, 9000000000135, 'ATTR000075', '频率', 'STRING', NULL, false, true, NULL, 75, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000135 AND code = 'ATTR000075');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000312, 9000000000135, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000135 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000313, 9000000000135, 'ATTR000077', '容量', 'STRING', NULL, false, true, NULL, 77, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000135 AND code = 'ATTR000077');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000314, 9000000000135, 'ATTR000091', '外壳', 'STRING', NULL, false, true, NULL, 91, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000135 AND code = 'ATTR000091');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000315, 9000000000135, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000135 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000316, 9000000000135, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000135 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000317, 9000000000135, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000135 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000318, 9000000000136, 'ATTR000011', '材质', 'STRING', NULL, false, true, NULL, 11, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000136 AND code = 'ATTR000011');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000319, 9000000000136, 'ATTR000027', '电压变比', 'STRING', NULL, false, true, NULL, 27, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000136 AND code = 'ATTR000027');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000320, 9000000000136, 'ATTR000071', '冷却方式', 'STRING', NULL, false, true, NULL, 71, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000136 AND code = 'ATTR000071');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000321, 9000000000136, 'ATTR000075', '频率', 'STRING', NULL, false, true, NULL, 75, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000136 AND code = 'ATTR000075');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000322, 9000000000136, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000136 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000323, 9000000000136, 'ATTR000077', '容量', 'STRING', NULL, false, true, NULL, 77, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000136 AND code = 'ATTR000077');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000324, 9000000000136, 'ATTR000091', '外壳', 'STRING', NULL, false, true, NULL, 91, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000136 AND code = 'ATTR000091');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000325, 9000000000136, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000136 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000326, 9000000000136, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000136 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000327, 9000000000136, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000136 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000328, 9000000000137, 'ATTR000011', '材质', 'STRING', NULL, false, true, NULL, 11, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000137 AND code = 'ATTR000011');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000329, 9000000000137, 'ATTR000027', '电压变比', 'STRING', NULL, false, true, NULL, 27, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000137 AND code = 'ATTR000027');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000330, 9000000000137, 'ATTR000071', '冷却方式', 'STRING', NULL, false, true, NULL, 71, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000137 AND code = 'ATTR000071');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000331, 9000000000137, 'ATTR000075', '频率', 'STRING', NULL, false, true, NULL, 75, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000137 AND code = 'ATTR000075');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000332, 9000000000137, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000137 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000333, 9000000000137, 'ATTR000077', '容量', 'STRING', NULL, false, true, NULL, 77, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000137 AND code = 'ATTR000077');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000334, 9000000000137, 'ATTR000091', '外壳', 'STRING', NULL, false, true, NULL, 91, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000137 AND code = 'ATTR000091');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000335, 9000000000137, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000137 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000336, 9000000000137, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000137 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000337, 9000000000137, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000137 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000338, 9000000000138, 'ATTR000011', '材质', 'STRING', NULL, false, true, NULL, 11, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000138 AND code = 'ATTR000011');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000339, 9000000000138, 'ATTR000027', '电压变比', 'STRING', NULL, false, true, NULL, 27, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000138 AND code = 'ATTR000027');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000340, 9000000000138, 'ATTR000071', '冷却方式', 'STRING', NULL, false, true, NULL, 71, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000138 AND code = 'ATTR000071');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000341, 9000000000138, 'ATTR000075', '频率', 'STRING', NULL, false, true, NULL, 75, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000138 AND code = 'ATTR000075');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000342, 9000000000138, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000138 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000343, 9000000000138, 'ATTR000077', '容量', 'STRING', NULL, false, true, NULL, 77, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000138 AND code = 'ATTR000077');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000344, 9000000000138, 'ATTR000091', '外壳', 'STRING', NULL, false, true, NULL, 91, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000138 AND code = 'ATTR000091');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000345, 9000000000138, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000138 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000346, 9000000000138, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000138 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000347, 9000000000138, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000138 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000348, 9000000000139, 'ATTR000001', '安装方式', 'STRING', NULL, false, true, NULL, 1, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000139 AND code = 'ATTR000001');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000349, 9000000000139, 'ATTR000069', '控制电源电压', 'STRING', NULL, false, true, NULL, 69, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000139 AND code = 'ATTR000069');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000350, 9000000000139, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000139 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000351, 9000000000139, 'ATTR000099', '延时范围', 'STRING', NULL, false, true, NULL, 99, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000139 AND code = 'ATTR000099');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000352, 9000000000139, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000139 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000353, 9000000000139, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000139 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000354, 9000000000139, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000139 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000355, 9000000000140, 'ATTR000001', '安装方式', 'STRING', NULL, false, true, NULL, 1, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000140 AND code = 'ATTR000001');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000356, 9000000000140, 'ATTR000069', '控制电源电压', 'STRING', NULL, false, true, NULL, 69, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000140 AND code = 'ATTR000069');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000357, 9000000000140, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000140 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000358, 9000000000140, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000140 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000359, 9000000000140, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000140 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000360, 9000000000140, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000140 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000361, 9000000000141, 'ATTR000001', '安装方式', 'STRING', NULL, false, true, NULL, 1, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000141 AND code = 'ATTR000001');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000362, 9000000000141, 'ATTR000069', '控制电源电压', 'STRING', NULL, false, true, NULL, 69, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000141 AND code = 'ATTR000069');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000363, 9000000000141, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000141 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000364, 9000000000141, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000141 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000365, 9000000000141, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000141 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000366, 9000000000141, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000141 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000367, 9000000000142, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000142 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000368, 9000000000142, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000142 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000369, 9000000000142, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000142 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000370, 9000000000142, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000142 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000371, 9000000000143, 'ATTR000001', '安装方式', 'STRING', NULL, false, true, NULL, 1, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000143 AND code = 'ATTR000001');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000372, 9000000000143, 'ATTR000040', '额定频率', 'STRING', NULL, false, true, NULL, 40, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000143 AND code = 'ATTR000040');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000373, 9000000000143, 'ATTR000067', '壳架电流', 'STRING', NULL, false, true, NULL, 67, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000143 AND code = 'ATTR000067');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000374, 9000000000143, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000143 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000375, 9000000000143, 'ATTR000103', '整定电流范围', 'STRING', NULL, false, true, NULL, 103, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000143 AND code = 'ATTR000103');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000376, 9000000000143, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000143 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000377, 9000000000143, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000143 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000378, 9000000000143, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000143 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000379, 9000000000144, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000144 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000380, 9000000000144, 'ATTR000027', '电压变比', 'STRING', NULL, false, true, NULL, 27, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000144 AND code = 'ATTR000027');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000381, 9000000000144, 'ATTR000056', '供电电源', 'STRING', NULL, false, true, NULL, 56, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000144 AND code = 'ATTR000056');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000382, 9000000000144, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000144 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000383, 9000000000144, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000144 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000384, 9000000000144, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000144 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000385, 9000000000144, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000144 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000386, 9000000000145, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000145 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000387, 9000000000145, 'ATTR000022', '电流变比', 'STRING', NULL, false, true, NULL, 22, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000145 AND code = 'ATTR000022');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000388, 9000000000145, 'ATTR000056', '供电电源', 'STRING', NULL, false, true, NULL, 56, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000145 AND code = 'ATTR000056');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000389, 9000000000145, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000145 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000390, 9000000000145, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000145 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000391, 9000000000145, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000145 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000392, 9000000000145, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000145 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000393, 9000000000146, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000146 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000394, 9000000000146, 'ATTR000023', '电流电压变比', 'STRING', NULL, false, true, NULL, 23, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000146 AND code = 'ATTR000023');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000395, 9000000000146, 'ATTR000056', '供电电源', 'STRING', NULL, false, true, NULL, 56, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000146 AND code = 'ATTR000056');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000396, 9000000000146, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000146 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000397, 9000000000146, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000146 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000398, 9000000000146, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000146 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000399, 9000000000146, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000146 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000400, 9000000000147, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000147 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000401, 9000000000147, 'ATTR000056', '供电电源', 'STRING', NULL, false, true, NULL, 56, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000147 AND code = 'ATTR000056');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000402, 9000000000147, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000147 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000403, 9000000000147, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000147 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000404, 9000000000147, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000147 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000405, 9000000000147, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000147 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000406, 9000000000148, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000148 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000407, 9000000000148, 'ATTR000056', '供电电源', 'STRING', NULL, false, true, NULL, 56, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000148 AND code = 'ATTR000056');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000408, 9000000000148, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000148 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000409, 9000000000148, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000148 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000410, 9000000000148, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000148 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000411, 9000000000148, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000148 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000412, 9000000000149, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000149 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000413, 9000000000149, 'ATTR000056', '供电电源', 'STRING', NULL, false, true, NULL, 56, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000149 AND code = 'ATTR000056');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000414, 9000000000149, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000149 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000415, 9000000000149, 'ATTR000095', '相位', 'STRING', NULL, false, true, NULL, 95, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000149 AND code = 'ATTR000095');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000416, 9000000000149, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000149 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000417, 9000000000149, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000149 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000418, 9000000000149, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000149 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000419, 9000000000150, 'ATTR000025', '电流值', 'STRING', NULL, false, true, NULL, 25, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000150 AND code = 'ATTR000025');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000420, 9000000000150, 'ATTR000030', '电压值', 'STRING', NULL, false, true, NULL, 30, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000150 AND code = 'ATTR000030');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000421, 9000000000150, 'ATTR000052', '工作方式', 'STRING', NULL, false, true, NULL, 52, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000150 AND code = 'ATTR000052');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000422, 9000000000150, 'ATTR000056', '供电电源', 'STRING', NULL, false, true, NULL, 56, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000150 AND code = 'ATTR000056');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000423, 9000000000150, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000150 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000424, 9000000000150, 'ATTR000105', '准确级', 'STRING', NULL, false, true, NULL, 105, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000150 AND code = 'ATTR000105');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000425, 9000000000150, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000150 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000426, 9000000000150, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000150 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000427, 9000000000150, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000150 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000428, 9000000000151, 'ATTR000003', '报警值设置范围', 'STRING', NULL, false, true, NULL, 3, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000151 AND code = 'ATTR000003');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000429, 9000000000151, 'ATTR000029', '电压范围', 'STRING', NULL, false, true, NULL, 29, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000151 AND code = 'ATTR000029');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000430, 9000000000151, 'ATTR000073', '灵敏度', 'STRING', NULL, false, true, NULL, 73, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000151 AND code = 'ATTR000073');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000431, 9000000000151, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000151 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000432, 9000000000151, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000151 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000433, 9000000000151, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000151 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000434, 9000000000151, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000151 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000435, 9000000000152, 'ATTR000001', '安装方式', 'STRING', NULL, false, true, NULL, 1, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000152 AND code = 'ATTR000001');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000436, 9000000000152, 'ATTR000052', '工作方式', 'STRING', NULL, false, true, NULL, 52, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000152 AND code = 'ATTR000052');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000437, 9000000000152, 'ATTR000056', '供电电源', 'STRING', NULL, false, true, NULL, 56, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000152 AND code = 'ATTR000056');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000438, 9000000000152, 'ATTR000064', '精度', 'STRING', NULL, false, true, NULL, 64, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000152 AND code = 'ATTR000064');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000439, 9000000000152, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000152 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000440, 9000000000152, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000152 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000441, 9000000000152, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000152 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000442, 9000000000152, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000152 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000443, 9000000000153, 'ATTR000025', '电流值', 'STRING', NULL, false, true, NULL, 25, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000153 AND code = 'ATTR000025');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000444, 9000000000153, 'ATTR000030', '电压值', 'STRING', NULL, false, true, NULL, 30, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000153 AND code = 'ATTR000030');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000445, 9000000000153, 'ATTR000052', '工作方式', 'STRING', NULL, false, true, NULL, 52, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000153 AND code = 'ATTR000052');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000446, 9000000000153, 'ATTR000056', '供电电源', 'STRING', NULL, false, true, NULL, 56, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000153 AND code = 'ATTR000056');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000447, 9000000000153, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000153 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000448, 9000000000153, 'ATTR000105', '准确级', 'STRING', NULL, false, true, NULL, 105, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000153 AND code = 'ATTR000105');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000449, 9000000000153, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000153 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000450, 9000000000153, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000153 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000451, 9000000000153, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000153 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000452, 9000000000154, 'ATTR000001', '安装方式', 'STRING', NULL, false, true, NULL, 1, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000154 AND code = 'ATTR000001');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000453, 9000000000154, 'ATTR000051', '工作范围', 'STRING', NULL, false, true, NULL, 51, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000154 AND code = 'ATTR000051');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000454, 9000000000154, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000154 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000455, 9000000000154, 'ATTR000098', '压力范围', 'STRING', NULL, false, true, NULL, 98, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000154 AND code = 'ATTR000098');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000456, 9000000000154, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000154 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000457, 9000000000154, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000154 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000458, 9000000000154, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000154 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000459, 9000000000155, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000155 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000460, 9000000000155, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000155 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000461, 9000000000155, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000155 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000462, 9000000000155, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000155 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000463, 9000000000156, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000156 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000464, 9000000000156, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000156 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000465, 9000000000156, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000156 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000466, 9000000000156, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000156 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000467, 9000000000157, 'ATTR000056', '供电电源', 'STRING', NULL, false, true, NULL, 56, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000157 AND code = 'ATTR000056');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000468, 9000000000157, 'ATTR000064', '精度', 'STRING', NULL, false, true, NULL, 64, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000157 AND code = 'ATTR000064');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000469, 9000000000157, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000157 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000470, 9000000000157, 'ATTR000092', '温度范围', 'STRING', NULL, false, true, NULL, 92, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000157 AND code = 'ATTR000092');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000471, 9000000000157, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000157 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000472, 9000000000157, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000157 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000473, 9000000000157, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000157 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000474, 9000000000158, 'ATTR000048', '辅头形式', 'STRING', NULL, false, true, NULL, 48, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000158 AND code = 'ATTR000048');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000475, 9000000000158, 'ATTR000050', '工作电压', 'STRING', NULL, false, true, NULL, 50, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000158 AND code = 'ATTR000050');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000476, 9000000000158, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000158 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000477, 9000000000158, 'ATTR000100', '颜色', 'STRING', NULL, false, true, NULL, 100, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000158 AND code = 'ATTR000100');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000478, 9000000000158, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000158 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000479, 9000000000158, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000158 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000480, 9000000000158, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000158 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000481, 9000000000159, 'ATTR000048', '辅头形式', 'STRING', NULL, false, true, NULL, 48, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000159 AND code = 'ATTR000048');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000482, 9000000000159, 'ATTR000050', '工作电压', 'STRING', NULL, false, true, NULL, 50, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000159 AND code = 'ATTR000050');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000483, 9000000000159, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000159 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000484, 9000000000159, 'ATTR000100', '颜色', 'STRING', NULL, false, true, NULL, 100, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000159 AND code = 'ATTR000100');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000485, 9000000000159, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000159 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000486, 9000000000159, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000159 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000487, 9000000000159, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000159 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000488, 9000000000160, 'ATTR000014', '操作类型', 'STRING', NULL, false, true, NULL, 14, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000160 AND code = 'ATTR000014');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000489, 9000000000160, 'ATTR000015', '操作图编号', 'STRING', NULL, false, true, NULL, 15, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000160 AND code = 'ATTR000015');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000490, 9000000000160, 'ATTR000041', '发热电流', 'STRING', NULL, false, true, NULL, 41, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000160 AND code = 'ATTR000041');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000491, 9000000000160, 'ATTR000062', '节数', 'STRING', NULL, false, true, NULL, 62, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000160 AND code = 'ATTR000062');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000492, 9000000000160, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000160 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000493, 9000000000160, 'ATTR000104', '转换角度', 'STRING', NULL, false, true, NULL, 104, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000160 AND code = 'ATTR000104');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000494, 9000000000160, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000160 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000495, 9000000000160, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000160 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000496, 9000000000160, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000160 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000497, 9000000000161, 'ATTR000050', '工作电压', 'STRING', NULL, false, true, NULL, 50, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000161 AND code = 'ATTR000050');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000498, 9000000000161, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000161 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000499, 9000000000161, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000161 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000500, 9000000000161, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000161 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000501, 9000000000161, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000161 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000502, 9000000000162, 'ATTR000001', '安装方式', 'STRING', NULL, false, true, NULL, 1, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000162 AND code = 'ATTR000001');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000503, 9000000000162, 'ATTR000029', '电压范围', 'STRING', NULL, false, true, NULL, 29, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000162 AND code = 'ATTR000029');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000504, 9000000000162, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000162 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000505, 9000000000162, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000162 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000506, 9000000000162, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000162 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000507, 9000000000162, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000162 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000508, 9000000000163, 'ATTR000013', '操作方式', 'STRING', NULL, false, true, NULL, 13, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000163 AND code = 'ATTR000013');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000509, 9000000000163, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000163 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000510, 9000000000163, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000163 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000511, 9000000000163, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000163 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000512, 9000000000163, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000163 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000513, 9000000000163, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000163 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000514, 9000000000164, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000164 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000515, 9000000000164, 'ATTR000092', '温度范围', 'STRING', NULL, false, true, NULL, 92, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000164 AND code = 'ATTR000092');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000516, 9000000000164, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000164 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000517, 9000000000164, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000164 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000518, 9000000000164, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000164 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000519, 9000000000165, 'ATTR000013', '操作方式', 'STRING', NULL, false, true, NULL, 13, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000165 AND code = 'ATTR000013');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000520, 9000000000165, 'ATTR000018', '触头形式', 'STRING', NULL, false, true, NULL, 18, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000165 AND code = 'ATTR000018');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000521, 9000000000165, 'ATTR000033', '动作力', 'STRING', NULL, false, true, NULL, 33, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000165 AND code = 'ATTR000033');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000522, 9000000000165, 'ATTR000034', '动作行程', 'STRING', NULL, false, true, NULL, 34, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000165 AND code = 'ATTR000034');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000523, 9000000000165, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000165 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000524, 9000000000165, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000165 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000525, 9000000000165, 'ATTR000106', '总行程', 'STRING', NULL, false, true, NULL, 106, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000165 AND code = 'ATTR000106');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000526, 9000000000165, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000165 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000527, 9000000000165, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000165 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000528, 9000000000165, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000165 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000529, 9000000000166, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000166 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000530, 9000000000166, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000166 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000531, 9000000000166, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000166 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000532, 9000000000166, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000166 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000533, 9000000000167, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000167 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000534, 9000000000167, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000167 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000535, 9000000000167, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000167 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000536, 9000000000167, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000167 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000537, 9000000000168, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000168 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000538, 9000000000168, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000168 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000539, 9000000000168, 'ATTR000100', '颜色', 'STRING', NULL, false, true, NULL, 100, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000168 AND code = 'ATTR000100');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000540, 9000000000168, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000168 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000541, 9000000000168, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000168 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000542, 9000000000168, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000168 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000543, 9000000000169, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000169 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000544, 9000000000169, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000169 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000545, 9000000000169, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000169 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000546, 9000000000169, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000169 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000547, 9000000000170, 'ATTR000050', '工作电压', 'STRING', NULL, false, true, NULL, 50, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000170 AND code = 'ATTR000050');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000548, 9000000000170, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000170 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000549, 9000000000170, 'ATTR000100', '颜色', 'STRING', NULL, false, true, NULL, 100, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000170 AND code = 'ATTR000100');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000550, 9000000000170, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000170 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000551, 9000000000170, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000170 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000552, 9000000000170, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000170 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000553, 9000000000171, 'ATTR000056', '供电电源', 'STRING', NULL, false, true, NULL, 56, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000171 AND code = 'ATTR000056');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000554, 9000000000171, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000171 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000555, 9000000000171, 'ATTR000083', '输出电压', 'STRING', NULL, false, true, NULL, 83, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000171 AND code = 'ATTR000083');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000556, 9000000000171, 'ATTR000086', '输入电压', 'STRING', NULL, false, true, NULL, 86, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000171 AND code = 'ATTR000086');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000557, 9000000000171, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000171 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000558, 9000000000171, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000171 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000559, 9000000000171, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000171 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000560, 9000000000172, 'ATTR000056', '供电电源', 'STRING', NULL, false, true, NULL, 56, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000172 AND code = 'ATTR000056');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000561, 9000000000172, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000172 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000562, 9000000000172, 'ATTR000082', '输出电流', 'STRING', NULL, false, true, NULL, 82, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000172 AND code = 'ATTR000082');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000563, 9000000000172, 'ATTR000085', '输入电流', 'STRING', NULL, false, true, NULL, 85, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000172 AND code = 'ATTR000085');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000564, 9000000000172, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000172 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000565, 9000000000172, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000172 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000566, 9000000000172, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000172 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000567, 9000000000173, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000173 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000568, 9000000000173, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000173 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000569, 9000000000173, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000173 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000570, 9000000000173, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000173 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000571, 9000000000174, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000174 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000572, 9000000000174, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000174 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000573, 9000000000174, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000174 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000574, 9000000000174, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000174 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000575, 9000000000175, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000175 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000576, 9000000000175, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000175 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000577, 9000000000175, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000175 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000578, 9000000000175, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000175 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000579, 9000000000176, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000176 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000580, 9000000000176, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000176 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000581, 9000000000176, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000176 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000582, 9000000000176, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000176 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000583, 9000000000177, 'ATTR000009', '标称阻值', 'STRING', NULL, false, true, NULL, 9, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000177 AND code = 'ATTR000009');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000584, 9000000000177, 'ATTR000046', '封装', 'STRING', NULL, false, true, NULL, 46, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000177 AND code = 'ATTR000046');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000585, 9000000000177, 'ATTR000055', '功率', 'STRING', NULL, false, true, NULL, 55, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000177 AND code = 'ATTR000055');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000586, 9000000000177, 'ATTR000064', '精度', 'STRING', NULL, false, true, NULL, 64, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000177 AND code = 'ATTR000064');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000587, 9000000000177, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000177 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000588, 9000000000177, 'ATTR000093', '温度系数', 'STRING', NULL, false, true, NULL, 93, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000177 AND code = 'ATTR000093');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000589, 9000000000177, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000177 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000590, 9000000000177, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000177 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000591, 9000000000177, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000177 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000592, 9000000000178, 'ATTR000009', '标称阻值', 'STRING', NULL, false, true, NULL, 9, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000178 AND code = 'ATTR000009');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000593, 9000000000178, 'ATTR000046', '封装', 'STRING', NULL, false, true, NULL, 46, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000178 AND code = 'ATTR000046');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000594, 9000000000178, 'ATTR000055', '功率', 'STRING', NULL, false, true, NULL, 55, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000178 AND code = 'ATTR000055');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000595, 9000000000178, 'ATTR000064', '精度', 'STRING', NULL, false, true, NULL, 64, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000178 AND code = 'ATTR000064');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000596, 9000000000178, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000178 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000597, 9000000000178, 'ATTR000093', '温度系数', 'STRING', NULL, false, true, NULL, 93, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000178 AND code = 'ATTR000093');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000598, 9000000000178, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000178 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000599, 9000000000178, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000178 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000600, 9000000000178, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000178 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000601, 9000000000179, 'ATTR000009', '标称阻值', 'STRING', NULL, false, true, NULL, 9, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000179 AND code = 'ATTR000009');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000602, 9000000000179, 'ATTR000031', '电阻范围', 'STRING', NULL, false, true, NULL, 31, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000179 AND code = 'ATTR000031');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000603, 9000000000179, 'ATTR000046', '封装', 'STRING', NULL, false, true, NULL, 46, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000179 AND code = 'ATTR000046');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000604, 9000000000179, 'ATTR000055', '功率', 'STRING', NULL, false, true, NULL, 55, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000179 AND code = 'ATTR000055');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000605, 9000000000179, 'ATTR000064', '精度', 'STRING', NULL, false, true, NULL, 64, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000179 AND code = 'ATTR000064');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000606, 9000000000179, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000179 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000607, 9000000000179, 'ATTR000093', '温度系数', 'STRING', NULL, false, true, NULL, 93, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000179 AND code = 'ATTR000093');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000608, 9000000000179, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000179 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000609, 9000000000179, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000179 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000610, 9000000000179, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000179 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000611, 9000000000180, 'ATTR000005', '标称电压', 'STRING', NULL, false, true, NULL, 5, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000180 AND code = 'ATTR000005');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000612, 9000000000180, 'ATTR000026', '电压比', 'STRING', NULL, false, true, NULL, 26, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000180 AND code = 'ATTR000026');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000613, 9000000000180, 'ATTR000046', '封装', 'STRING', NULL, false, true, NULL, 46, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000180 AND code = 'ATTR000046');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000614, 9000000000180, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000180 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000615, 9000000000180, 'ATTR000093', '温度系数', 'STRING', NULL, false, true, NULL, 93, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000180 AND code = 'ATTR000093');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000616, 9000000000180, 'ATTR000110', '最大限制电压', 'STRING', NULL, false, true, NULL, 110, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000180 AND code = 'ATTR000110');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000617, 9000000000180, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000180 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000618, 9000000000180, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000180 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000619, 9000000000180, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000180 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000620, 9000000000181, 'ATTR000001', '安装方式', 'STRING', NULL, false, true, NULL, 1, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000181 AND code = 'ATTR000001');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000621, 9000000000181, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000181 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000622, 9000000000181, 'ATTR000046', '封装', 'STRING', NULL, false, true, NULL, 46, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000181 AND code = 'ATTR000046');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000623, 9000000000181, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000181 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000624, 9000000000181, 'ATTR000093', '温度系数', 'STRING', NULL, false, true, NULL, 93, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000181 AND code = 'ATTR000093');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000625, 9000000000181, 'ATTR000107', '阻值范围', 'STRING', NULL, false, true, NULL, 107, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000181 AND code = 'ATTR000107');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000626, 9000000000181, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000181 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000627, 9000000000181, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000181 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000628, 9000000000181, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000181 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000629, 9000000000182, 'ATTR000009', '标称阻值', 'STRING', NULL, false, true, NULL, 9, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000182 AND code = 'ATTR000009');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000630, 9000000000182, 'ATTR000046', '封装', 'STRING', NULL, false, true, NULL, 46, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000182 AND code = 'ATTR000046');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000631, 9000000000182, 'ATTR000055', '功率', 'STRING', NULL, false, true, NULL, 55, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000182 AND code = 'ATTR000055');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000632, 9000000000182, 'ATTR000064', '精度', 'STRING', NULL, false, true, NULL, 64, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000182 AND code = 'ATTR000064');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000633, 9000000000182, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000182 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000634, 9000000000182, 'ATTR000093', '温度系数', 'STRING', NULL, false, true, NULL, 93, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000182 AND code = 'ATTR000093');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000635, 9000000000182, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000182 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000636, 9000000000182, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000182 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000637, 9000000000182, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000182 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000638, 9000000000183, 'ATTR000009', '标称阻值', 'STRING', NULL, false, true, NULL, 9, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000183 AND code = 'ATTR000009');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000639, 9000000000183, 'ATTR000046', '封装', 'STRING', NULL, false, true, NULL, 46, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000183 AND code = 'ATTR000046');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000640, 9000000000183, 'ATTR000055', '功率', 'STRING', NULL, false, true, NULL, 55, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000183 AND code = 'ATTR000055');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000641, 9000000000183, 'ATTR000064', '精度', 'STRING', NULL, false, true, NULL, 64, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000183 AND code = 'ATTR000064');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000642, 9000000000183, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000183 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000643, 9000000000183, 'ATTR000093', '温度系数', 'STRING', NULL, false, true, NULL, 93, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000183 AND code = 'ATTR000093');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000644, 9000000000183, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000183 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000645, 9000000000183, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000183 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000646, 9000000000183, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000183 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000647, 9000000000184, 'ATTR000009', '标称阻值', 'STRING', NULL, false, true, NULL, 9, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000184 AND code = 'ATTR000009');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000648, 9000000000184, 'ATTR000046', '封装', 'STRING', NULL, false, true, NULL, 46, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000184 AND code = 'ATTR000046');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000649, 9000000000184, 'ATTR000055', '功率', 'STRING', NULL, false, true, NULL, 55, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000184 AND code = 'ATTR000055');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000650, 9000000000184, 'ATTR000064', '精度', 'STRING', NULL, false, true, NULL, 64, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000184 AND code = 'ATTR000064');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000651, 9000000000184, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000184 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000652, 9000000000184, 'ATTR000093', '温度系数', 'STRING', NULL, false, true, NULL, 93, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000184 AND code = 'ATTR000093');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000653, 9000000000184, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000184 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000654, 9000000000184, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000184 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000655, 9000000000184, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000184 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000656, 9000000000185, 'ATTR000008', '标称容量', 'STRING', NULL, false, true, NULL, 8, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000185 AND code = 'ATTR000008');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000657, 9000000000185, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000185 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000658, 9000000000185, 'ATTR000046', '封装', 'STRING', NULL, false, true, NULL, 46, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000185 AND code = 'ATTR000046');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000659, 9000000000185, 'ATTR000054', '工作温度', 'STRING', NULL, false, true, NULL, 54, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000185 AND code = 'ATTR000054');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000660, 9000000000185, 'ATTR000060', '脚距', 'STRING', NULL, false, true, NULL, 60, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000185 AND code = 'ATTR000060');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000661, 9000000000185, 'ATTR000065', '精度等级', 'STRING', NULL, false, true, NULL, 65, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000185 AND code = 'ATTR000065');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000662, 9000000000185, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000185 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000663, 9000000000185, 'ATTR000078', '容量误差', 'STRING', NULL, false, true, NULL, 78, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000185 AND code = 'ATTR000078');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000664, 9000000000185, 'ATTR000081', '使用寿命', 'STRING', NULL, false, true, NULL, 81, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000185 AND code = 'ATTR000081');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000665, 9000000000185, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000185 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000666, 9000000000185, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000185 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000667, 9000000000185, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000185 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000668, 9000000000186, 'ATTR000008', '标称容量', 'STRING', NULL, false, true, NULL, 8, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000186 AND code = 'ATTR000008');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000669, 9000000000186, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000186 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000670, 9000000000186, 'ATTR000046', '封装', 'STRING', NULL, false, true, NULL, 46, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000186 AND code = 'ATTR000046');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000671, 9000000000186, 'ATTR000054', '工作温度', 'STRING', NULL, false, true, NULL, 54, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000186 AND code = 'ATTR000054');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000672, 9000000000186, 'ATTR000060', '脚距', 'STRING', NULL, false, true, NULL, 60, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000186 AND code = 'ATTR000060');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000673, 9000000000186, 'ATTR000065', '精度等级', 'STRING', NULL, false, true, NULL, 65, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000186 AND code = 'ATTR000065');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000674, 9000000000186, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000186 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000675, 9000000000186, 'ATTR000078', '容量误差', 'STRING', NULL, false, true, NULL, 78, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000186 AND code = 'ATTR000078');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000676, 9000000000186, 'ATTR000081', '使用寿命', 'STRING', NULL, false, true, NULL, 81, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000186 AND code = 'ATTR000081');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000677, 9000000000186, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000186 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000678, 9000000000186, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000186 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000679, 9000000000186, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000186 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000680, 9000000000187, 'ATTR000008', '标称容量', 'STRING', NULL, false, true, NULL, 8, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000187 AND code = 'ATTR000008');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000681, 9000000000187, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000187 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000682, 9000000000187, 'ATTR000046', '封装', 'STRING', NULL, false, true, NULL, 46, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000187 AND code = 'ATTR000046');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000683, 9000000000187, 'ATTR000054', '工作温度', 'STRING', NULL, false, true, NULL, 54, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000187 AND code = 'ATTR000054');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000684, 9000000000187, 'ATTR000060', '脚距', 'STRING', NULL, false, true, NULL, 60, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000187 AND code = 'ATTR000060');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000685, 9000000000187, 'ATTR000065', '精度等级', 'STRING', NULL, false, true, NULL, 65, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000187 AND code = 'ATTR000065');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000686, 9000000000187, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000187 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000687, 9000000000187, 'ATTR000078', '容量误差', 'STRING', NULL, false, true, NULL, 78, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000187 AND code = 'ATTR000078');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000688, 9000000000187, 'ATTR000081', '使用寿命', 'STRING', NULL, false, true, NULL, 81, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000187 AND code = 'ATTR000081');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000689, 9000000000187, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000187 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000690, 9000000000187, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000187 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000691, 9000000000187, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000187 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000692, 9000000000188, 'ATTR000008', '标称容量', 'STRING', NULL, false, true, NULL, 8, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000188 AND code = 'ATTR000008');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000693, 9000000000188, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000188 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000694, 9000000000188, 'ATTR000046', '封装', 'STRING', NULL, false, true, NULL, 46, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000188 AND code = 'ATTR000046');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000695, 9000000000188, 'ATTR000054', '工作温度', 'STRING', NULL, false, true, NULL, 54, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000188 AND code = 'ATTR000054');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000696, 9000000000188, 'ATTR000060', '脚距', 'STRING', NULL, false, true, NULL, 60, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000188 AND code = 'ATTR000060');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000697, 9000000000188, 'ATTR000065', '精度等级', 'STRING', NULL, false, true, NULL, 65, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000188 AND code = 'ATTR000065');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000698, 9000000000188, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000188 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000699, 9000000000188, 'ATTR000078', '容量误差', 'STRING', NULL, false, true, NULL, 78, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000188 AND code = 'ATTR000078');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000700, 9000000000188, 'ATTR000081', '使用寿命', 'STRING', NULL, false, true, NULL, 81, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000188 AND code = 'ATTR000081');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000701, 9000000000188, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000188 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000702, 9000000000188, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000188 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000703, 9000000000188, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000188 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000704, 9000000000189, 'ATTR000008', '标称容量', 'STRING', NULL, false, true, NULL, 8, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000189 AND code = 'ATTR000008');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000705, 9000000000189, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000189 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000706, 9000000000189, 'ATTR000046', '封装', 'STRING', NULL, false, true, NULL, 46, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000189 AND code = 'ATTR000046');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000707, 9000000000189, 'ATTR000054', '工作温度', 'STRING', NULL, false, true, NULL, 54, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000189 AND code = 'ATTR000054');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000708, 9000000000189, 'ATTR000060', '脚距', 'STRING', NULL, false, true, NULL, 60, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000189 AND code = 'ATTR000060');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000709, 9000000000189, 'ATTR000065', '精度等级', 'STRING', NULL, false, true, NULL, 65, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000189 AND code = 'ATTR000065');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000710, 9000000000189, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000189 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000711, 9000000000189, 'ATTR000078', '容量误差', 'STRING', NULL, false, true, NULL, 78, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000189 AND code = 'ATTR000078');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000712, 9000000000189, 'ATTR000081', '使用寿命', 'STRING', NULL, false, true, NULL, 81, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000189 AND code = 'ATTR000081');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000713, 9000000000189, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000189 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000714, 9000000000189, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000189 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000715, 9000000000189, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000189 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000716, 9000000000190, 'ATTR000008', '标称容量', 'STRING', NULL, false, true, NULL, 8, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000190 AND code = 'ATTR000008');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000717, 9000000000190, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000190 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000718, 9000000000190, 'ATTR000046', '封装', 'STRING', NULL, false, true, NULL, 46, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000190 AND code = 'ATTR000046');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000719, 9000000000190, 'ATTR000054', '工作温度', 'STRING', NULL, false, true, NULL, 54, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000190 AND code = 'ATTR000054');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000720, 9000000000190, 'ATTR000060', '脚距', 'STRING', NULL, false, true, NULL, 60, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000190 AND code = 'ATTR000060');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000721, 9000000000190, 'ATTR000065', '精度等级', 'STRING', NULL, false, true, NULL, 65, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000190 AND code = 'ATTR000065');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000722, 9000000000190, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000190 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000723, 9000000000190, 'ATTR000078', '容量误差', 'STRING', NULL, false, true, NULL, 78, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000190 AND code = 'ATTR000078');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000724, 9000000000190, 'ATTR000081', '使用寿命', 'STRING', NULL, false, true, NULL, 81, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000190 AND code = 'ATTR000081');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000725, 9000000000190, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000190 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000726, 9000000000190, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000190 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000727, 9000000000190, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000190 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000728, 9000000000191, 'ATTR000008', '标称容量', 'STRING', NULL, false, true, NULL, 8, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000191 AND code = 'ATTR000008');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000729, 9000000000191, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000191 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000730, 9000000000191, 'ATTR000046', '封装', 'STRING', NULL, false, true, NULL, 46, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000191 AND code = 'ATTR000046');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000731, 9000000000191, 'ATTR000054', '工作温度', 'STRING', NULL, false, true, NULL, 54, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000191 AND code = 'ATTR000054');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000732, 9000000000191, 'ATTR000060', '脚距', 'STRING', NULL, false, true, NULL, 60, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000191 AND code = 'ATTR000060');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000733, 9000000000191, 'ATTR000065', '精度等级', 'STRING', NULL, false, true, NULL, 65, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000191 AND code = 'ATTR000065');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000734, 9000000000191, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000191 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000735, 9000000000191, 'ATTR000078', '容量误差', 'STRING', NULL, false, true, NULL, 78, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000191 AND code = 'ATTR000078');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000736, 9000000000191, 'ATTR000081', '使用寿命', 'STRING', NULL, false, true, NULL, 81, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000191 AND code = 'ATTR000081');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000737, 9000000000191, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000191 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000738, 9000000000191, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000191 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000739, 9000000000191, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000191 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000740, 9000000000192, 'ATTR000008', '标称容量', 'STRING', NULL, false, true, NULL, 8, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000192 AND code = 'ATTR000008');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000741, 9000000000192, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000192 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000742, 9000000000192, 'ATTR000046', '封装', 'STRING', NULL, false, true, NULL, 46, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000192 AND code = 'ATTR000046');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000743, 9000000000192, 'ATTR000054', '工作温度', 'STRING', NULL, false, true, NULL, 54, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000192 AND code = 'ATTR000054');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000744, 9000000000192, 'ATTR000060', '脚距', 'STRING', NULL, false, true, NULL, 60, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000192 AND code = 'ATTR000060');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000745, 9000000000192, 'ATTR000065', '精度等级', 'STRING', NULL, false, true, NULL, 65, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000192 AND code = 'ATTR000065');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000746, 9000000000192, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000192 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000747, 9000000000192, 'ATTR000078', '容量误差', 'STRING', NULL, false, true, NULL, 78, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000192 AND code = 'ATTR000078');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000748, 9000000000192, 'ATTR000081', '使用寿命', 'STRING', NULL, false, true, NULL, 81, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000192 AND code = 'ATTR000081');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000749, 9000000000192, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000192 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000750, 9000000000192, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000192 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000751, 9000000000192, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000192 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000752, 9000000000193, 'ATTR000008', '标称容量', 'STRING', NULL, false, true, NULL, 8, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000193 AND code = 'ATTR000008');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000753, 9000000000193, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000193 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000754, 9000000000193, 'ATTR000046', '封装', 'STRING', NULL, false, true, NULL, 46, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000193 AND code = 'ATTR000046');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000755, 9000000000193, 'ATTR000054', '工作温度', 'STRING', NULL, false, true, NULL, 54, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000193 AND code = 'ATTR000054');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000756, 9000000000193, 'ATTR000060', '脚距', 'STRING', NULL, false, true, NULL, 60, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000193 AND code = 'ATTR000060');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000757, 9000000000193, 'ATTR000065', '精度等级', 'STRING', NULL, false, true, NULL, 65, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000193 AND code = 'ATTR000065');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000758, 9000000000193, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000193 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000759, 9000000000193, 'ATTR000078', '容量误差', 'STRING', NULL, false, true, NULL, 78, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000193 AND code = 'ATTR000078');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000760, 9000000000193, 'ATTR000081', '使用寿命', 'STRING', NULL, false, true, NULL, 81, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000193 AND code = 'ATTR000081');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000761, 9000000000193, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000193 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000762, 9000000000193, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000193 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000763, 9000000000193, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000193 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000764, 9000000000194, 'ATTR000008', '标称容量', 'STRING', NULL, false, true, NULL, 8, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000194 AND code = 'ATTR000008');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000765, 9000000000194, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000194 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000766, 9000000000194, 'ATTR000046', '封装', 'STRING', NULL, false, true, NULL, 46, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000194 AND code = 'ATTR000046');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000767, 9000000000194, 'ATTR000054', '工作温度', 'STRING', NULL, false, true, NULL, 54, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000194 AND code = 'ATTR000054');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000768, 9000000000194, 'ATTR000060', '脚距', 'STRING', NULL, false, true, NULL, 60, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000194 AND code = 'ATTR000060');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000769, 9000000000194, 'ATTR000065', '精度等级', 'STRING', NULL, false, true, NULL, 65, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000194 AND code = 'ATTR000065');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000770, 9000000000194, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000194 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000771, 9000000000194, 'ATTR000078', '容量误差', 'STRING', NULL, false, true, NULL, 78, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000194 AND code = 'ATTR000078');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000772, 9000000000194, 'ATTR000081', '使用寿命', 'STRING', NULL, false, true, NULL, 81, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000194 AND code = 'ATTR000081');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000773, 9000000000194, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000194 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000774, 9000000000194, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000194 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000775, 9000000000194, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000194 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000776, 9000000000195, 'ATTR000008', '标称容量', 'STRING', NULL, false, true, NULL, 8, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000195 AND code = 'ATTR000008');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000777, 9000000000195, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000195 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000778, 9000000000195, 'ATTR000046', '封装', 'STRING', NULL, false, true, NULL, 46, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000195 AND code = 'ATTR000046');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000779, 9000000000195, 'ATTR000054', '工作温度', 'STRING', NULL, false, true, NULL, 54, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000195 AND code = 'ATTR000054');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000780, 9000000000195, 'ATTR000060', '脚距', 'STRING', NULL, false, true, NULL, 60, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000195 AND code = 'ATTR000060');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000781, 9000000000195, 'ATTR000065', '精度等级', 'STRING', NULL, false, true, NULL, 65, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000195 AND code = 'ATTR000065');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000782, 9000000000195, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000195 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000783, 9000000000195, 'ATTR000078', '容量误差', 'STRING', NULL, false, true, NULL, 78, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000195 AND code = 'ATTR000078');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000784, 9000000000195, 'ATTR000081', '使用寿命', 'STRING', NULL, false, true, NULL, 81, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000195 AND code = 'ATTR000081');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000785, 9000000000195, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000195 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000786, 9000000000195, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000195 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000787, 9000000000195, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000195 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000788, 9000000000196, 'ATTR000035', '额定电流', 'STRING', NULL, false, true, NULL, 35, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000196 AND code = 'ATTR000035');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000789, 9000000000196, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000196 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000790, 9000000000196, 'ATTR000047', '封装方式', 'STRING', NULL, false, true, NULL, 47, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000196 AND code = 'ATTR000047');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000791, 9000000000196, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000196 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000792, 9000000000196, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000196 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000793, 9000000000196, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000196 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000794, 9000000000196, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000196 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000795, 9000000000197, 'ATTR000035', '额定电流', 'STRING', NULL, false, true, NULL, 35, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000197 AND code = 'ATTR000035');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000796, 9000000000197, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000197 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000797, 9000000000197, 'ATTR000047', '封装方式', 'STRING', NULL, false, true, NULL, 47, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000197 AND code = 'ATTR000047');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000798, 9000000000197, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000197 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000799, 9000000000197, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000197 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000800, 9000000000197, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000197 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000801, 9000000000197, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000197 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000802, 9000000000198, 'ATTR000035', '额定电流', 'STRING', NULL, false, true, NULL, 35, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000198 AND code = 'ATTR000035');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000803, 9000000000198, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000198 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000804, 9000000000198, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000198 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000805, 9000000000198, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000198 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000806, 9000000000198, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000198 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000807, 9000000000198, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000198 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000808, 9000000000199, 'ATTR000035', '额定电流', 'STRING', NULL, false, true, NULL, 35, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000199 AND code = 'ATTR000035');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000809, 9000000000199, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000199 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000810, 9000000000199, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000199 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000811, 9000000000199, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000199 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000812, 9000000000199, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000199 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000813, 9000000000199, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000199 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000814, 9000000000200, 'ATTR000035', '额定电流', 'STRING', NULL, false, true, NULL, 35, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000200 AND code = 'ATTR000035');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000815, 9000000000200, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000200 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000816, 9000000000200, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000200 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000817, 9000000000200, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000200 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000818, 9000000000200, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000200 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000819, 9000000000200, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000200 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000820, 9000000000201, 'ATTR000035', '额定电流', 'STRING', NULL, false, true, NULL, 35, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000201 AND code = 'ATTR000035');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000821, 9000000000201, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000201 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000822, 9000000000201, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000201 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000823, 9000000000201, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000201 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000824, 9000000000201, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000201 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000825, 9000000000201, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000201 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000826, 9000000000202, 'ATTR000035', '额定电流', 'STRING', NULL, false, true, NULL, 35, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000202 AND code = 'ATTR000035');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000827, 9000000000202, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000202 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000828, 9000000000202, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000202 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000829, 9000000000202, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000202 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000830, 9000000000202, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000202 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000831, 9000000000202, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000202 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000832, 9000000000203, 'ATTR000035', '额定电流', 'STRING', NULL, false, true, NULL, 35, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000203 AND code = 'ATTR000035');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000833, 9000000000203, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000203 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000834, 9000000000203, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000203 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000835, 9000000000203, 'ATTR000082', '输出电流', 'STRING', NULL, false, true, NULL, 82, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000203 AND code = 'ATTR000082');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000836, 9000000000203, 'ATTR000083', '输出电压', 'STRING', NULL, false, true, NULL, 83, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000203 AND code = 'ATTR000083');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000837, 9000000000203, 'ATTR000086', '输入电压', 'STRING', NULL, false, true, NULL, 86, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000203 AND code = 'ATTR000086');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000838, 9000000000203, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000203 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000839, 9000000000203, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000203 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000840, 9000000000203, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000203 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000841, 9000000000204, 'ATTR000035', '额定电流', 'STRING', NULL, false, true, NULL, 35, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000204 AND code = 'ATTR000035');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000842, 9000000000204, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000204 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000843, 9000000000204, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000204 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000844, 9000000000204, 'ATTR000082', '输出电流', 'STRING', NULL, false, true, NULL, 82, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000204 AND code = 'ATTR000082');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000845, 9000000000204, 'ATTR000083', '输出电压', 'STRING', NULL, false, true, NULL, 83, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000204 AND code = 'ATTR000083');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000846, 9000000000204, 'ATTR000086', '输入电压', 'STRING', NULL, false, true, NULL, 86, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000204 AND code = 'ATTR000086');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000847, 9000000000204, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000204 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000848, 9000000000204, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000204 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000849, 9000000000204, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000204 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000850, 9000000000205, 'ATTR000035', '额定电流', 'STRING', NULL, false, true, NULL, 35, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000205 AND code = 'ATTR000035');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000851, 9000000000205, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000205 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000852, 9000000000205, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000205 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000853, 9000000000205, 'ATTR000082', '输出电流', 'STRING', NULL, false, true, NULL, 82, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000205 AND code = 'ATTR000082');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000854, 9000000000205, 'ATTR000083', '输出电压', 'STRING', NULL, false, true, NULL, 83, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000205 AND code = 'ATTR000083');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000855, 9000000000205, 'ATTR000086', '输入电压', 'STRING', NULL, false, true, NULL, 86, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000205 AND code = 'ATTR000086');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000856, 9000000000205, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000205 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000857, 9000000000205, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000205 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000858, 9000000000205, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000205 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000859, 9000000000206, 'ATTR000035', '额定电流', 'STRING', NULL, false, true, NULL, 35, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000206 AND code = 'ATTR000035');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000860, 9000000000206, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000206 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000861, 9000000000206, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000206 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000862, 9000000000206, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000206 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000863, 9000000000206, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000206 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000864, 9000000000206, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000206 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000865, 9000000000207, 'ATTR000035', '额定电流', 'STRING', NULL, false, true, NULL, 35, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000207 AND code = 'ATTR000035');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000866, 9000000000207, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000207 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000867, 9000000000207, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000207 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000868, 9000000000207, 'ATTR000082', '输出电流', 'STRING', NULL, false, true, NULL, 82, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000207 AND code = 'ATTR000082');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000869, 9000000000207, 'ATTR000083', '输出电压', 'STRING', NULL, false, true, NULL, 83, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000207 AND code = 'ATTR000083');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000870, 9000000000207, 'ATTR000086', '输入电压', 'STRING', NULL, false, true, NULL, 86, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000207 AND code = 'ATTR000086');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000871, 9000000000207, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000207 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000872, 9000000000207, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000207 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000873, 9000000000207, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000207 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000874, 9000000000208, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000208 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000875, 9000000000208, 'ATTR000096', '型号规格', 'STRING', NULL, false, true, NULL, 96, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000208 AND code = 'ATTR000096');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000876, 9000000000208, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000208 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000877, 9000000000208, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000208 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000878, 9000000000208, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000208 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000879, 9000000000209, 'ATTR000035', '额定电流', 'STRING', NULL, false, true, NULL, 35, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000209 AND code = 'ATTR000035');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000880, 9000000000209, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000209 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000881, 9000000000209, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000209 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000882, 9000000000209, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000209 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000883, 9000000000209, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000209 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000884, 9000000000209, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000209 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000885, 9000000000210, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000210 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000886, 9000000000210, 'ATTR000096', '型号规格', 'STRING', NULL, false, true, NULL, 96, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000210 AND code = 'ATTR000096');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000887, 9000000000210, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000210 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000888, 9000000000210, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000210 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000889, 9000000000210, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000210 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000890, 9000000000211, 'ATTR000035', '额定电流', 'STRING', NULL, false, true, NULL, 35, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000211 AND code = 'ATTR000035');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000891, 9000000000211, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000211 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000892, 9000000000211, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000211 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000893, 9000000000211, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000211 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000894, 9000000000211, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000211 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000895, 9000000000211, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000211 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000896, 9000000000212, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000212 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000897, 9000000000212, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000212 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000898, 9000000000212, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000212 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000899, 9000000000212, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000212 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000900, 9000000000212, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000212 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000901, 9000000000213, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000213 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000902, 9000000000213, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000213 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000903, 9000000000213, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000213 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000904, 9000000000213, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000213 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000905, 9000000000213, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000213 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000906, 9000000000214, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000214 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000907, 9000000000214, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000214 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000908, 9000000000214, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000214 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000909, 9000000000214, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000214 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000910, 9000000000214, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000214 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000911, 9000000000215, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000215 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000912, 9000000000215, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000215 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000913, 9000000000215, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000215 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000914, 9000000000215, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000215 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000915, 9000000000215, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000215 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000916, 9000000000216, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000216 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000917, 9000000000216, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000216 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000918, 9000000000216, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000216 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000919, 9000000000216, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000216 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000920, 9000000000216, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000216 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000921, 9000000000217, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000217 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000922, 9000000000217, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000217 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000923, 9000000000217, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000217 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000924, 9000000000217, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000217 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000925, 9000000000217, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000217 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000926, 9000000000218, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000218 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000927, 9000000000218, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000218 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000928, 9000000000218, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000218 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000929, 9000000000218, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000218 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000930, 9000000000218, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000218 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000931, 9000000000219, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000219 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000932, 9000000000219, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000219 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000933, 9000000000219, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000219 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000934, 9000000000219, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000219 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000935, 9000000000219, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000219 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000936, 9000000000220, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000220 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000937, 9000000000220, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000220 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000938, 9000000000220, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000220 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000939, 9000000000220, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000220 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000940, 9000000000220, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000220 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000941, 9000000000221, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000221 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000942, 9000000000221, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000221 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000943, 9000000000221, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000221 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000944, 9000000000221, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000221 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000945, 9000000000221, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000221 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000946, 9000000000222, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000222 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000947, 9000000000222, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000222 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000948, 9000000000222, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000222 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000949, 9000000000222, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000222 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000950, 9000000000222, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000222 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000951, 9000000000223, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000223 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000952, 9000000000223, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000223 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000953, 9000000000223, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000223 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000954, 9000000000223, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000223 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000955, 9000000000223, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000223 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000956, 9000000000224, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000224 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000957, 9000000000224, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000224 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000958, 9000000000224, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000224 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000959, 9000000000224, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000224 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000960, 9000000000224, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000224 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000961, 9000000000225, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000225 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000962, 9000000000225, 'ATTR000055', '功率', 'STRING', NULL, false, true, NULL, 55, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000225 AND code = 'ATTR000055');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000963, 9000000000225, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000225 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000964, 9000000000225, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000225 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000965, 9000000000225, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000225 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000966, 9000000000225, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000225 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000967, 9000000000226, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000226 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000968, 9000000000226, 'ATTR000053', '工作频率', 'STRING', NULL, false, true, NULL, 53, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000226 AND code = 'ATTR000053');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000969, 9000000000226, 'ATTR000055', '功率', 'STRING', NULL, false, true, NULL, 55, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000226 AND code = 'ATTR000055');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000970, 9000000000226, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000226 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000971, 9000000000226, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000226 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000972, 9000000000226, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000226 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000973, 9000000000226, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000226 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000974, 9000000000227, 'ATTR000019', '磁通量', 'STRING', NULL, false, true, NULL, 19, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000227 AND code = 'ATTR000019');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000975, 9000000000227, 'ATTR000053', '工作频率', 'STRING', NULL, false, true, NULL, 53, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000227 AND code = 'ATTR000053');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000976, 9000000000227, 'ATTR000054', '工作温度', 'STRING', NULL, false, true, NULL, 54, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000227 AND code = 'ATTR000054');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000977, 9000000000227, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000227 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000978, 9000000000227, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000227 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000979, 9000000000227, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000227 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000980, 9000000000227, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000227 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000981, 9000000000228, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000228 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000982, 9000000000228, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000228 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000983, 9000000000228, 'ATTR000077', '容量', 'STRING', NULL, false, true, NULL, 77, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000228 AND code = 'ATTR000077');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000984, 9000000000228, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000228 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000985, 9000000000228, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000228 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000986, 9000000000228, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000228 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000987, 9000000000229, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000229 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000988, 9000000000229, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000229 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000989, 9000000000229, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000229 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000990, 9000000000229, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000229 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000991, 9000000000231, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000231 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000992, 9000000000231, 'ATTR000055', '功率', 'STRING', NULL, false, true, NULL, 55, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000231 AND code = 'ATTR000055');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000993, 9000000000231, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000231 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000994, 9000000000231, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000231 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000995, 9000000000231, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000231 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000996, 9000000000231, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000231 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000997, 9000000000234, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000234 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000998, 9000000000234, 'ATTR000039', '额定功率', 'STRING', NULL, false, true, NULL, 39, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000234 AND code = 'ATTR000039');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000000999, 9000000000234, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000234 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001000, 9000000000234, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000234 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001001, 9000000000234, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000234 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001002, 9000000000234, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000234 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001003, 9000000000235, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000235 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001004, 9000000000235, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000235 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001005, 9000000000235, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000235 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001006, 9000000000235, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000235 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001007, 9000000000236, 'ATTR000011', '材质', 'STRING', NULL, false, true, NULL, 11, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000236 AND code = 'ATTR000011');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001008, 9000000000236, 'ATTR000042', '防爆类型', 'STRING', NULL, false, true, NULL, 42, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000236 AND code = 'ATTR000042');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001009, 9000000000236, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000236 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001010, 9000000000236, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000236 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001011, 9000000000236, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000236 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001012, 9000000000236, 'ATTR000115', '防爆等级', 'STRING', NULL, false, true, NULL, 115, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000236 AND code = 'ATTR000115');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001013, 9000000000237, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000237 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001014, 9000000000237, 'ATTR000055', '功率', 'STRING', NULL, false, true, NULL, 55, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000237 AND code = 'ATTR000055');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001015, 9000000000237, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000237 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001016, 9000000000237, 'ATTR000101', '应用类型', 'STRING', NULL, false, true, NULL, 101, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000237 AND code = 'ATTR000101');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001017, 9000000000237, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000237 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001018, 9000000000237, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000237 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001019, 9000000000237, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000237 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001020, 9000000000238, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000238 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001021, 9000000000238, 'ATTR000055', '功率', 'STRING', NULL, false, true, NULL, 55, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000238 AND code = 'ATTR000055');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001022, 9000000000238, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000238 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001023, 9000000000238, 'ATTR000101', '应用类型', 'STRING', NULL, false, true, NULL, 101, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000238 AND code = 'ATTR000101');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001024, 9000000000238, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000238 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001025, 9000000000238, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000238 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001026, 9000000000238, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000238 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001027, 9000000000239, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000239 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001028, 9000000000239, 'ATTR000055', '功率', 'STRING', NULL, false, true, NULL, 55, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000239 AND code = 'ATTR000055');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001029, 9000000000239, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000239 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001030, 9000000000239, 'ATTR000101', '应用类型', 'STRING', NULL, false, true, NULL, 101, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000239 AND code = 'ATTR000101');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001031, 9000000000239, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000239 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001032, 9000000000239, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000239 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001033, 9000000000239, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000239 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001034, 9000000000240, 'ATTR000011', '材质', 'STRING', NULL, false, true, NULL, 11, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000240 AND code = 'ATTR000011');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001035, 9000000000240, 'ATTR000043', '防护等级', 'STRING', NULL, false, true, NULL, 43, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000240 AND code = 'ATTR000043');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001036, 9000000000240, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000240 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001037, 9000000000240, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000240 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001038, 9000000000240, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000240 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001039, 9000000000241, 'ATTR000011', '材质', 'STRING', NULL, false, true, NULL, 11, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000241 AND code = 'ATTR000011');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001040, 9000000000241, 'ATTR000043', '防护等级', 'STRING', NULL, false, true, NULL, 43, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000241 AND code = 'ATTR000043');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001041, 9000000000241, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000241 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001042, 9000000000241, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000241 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001043, 9000000000241, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000241 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001044, 9000000000242, 'ATTR000036', '额定电压', 'STRING', NULL, false, true, NULL, 36, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000242 AND code = 'ATTR000036');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001045, 9000000000242, 'ATTR000055', '功率', 'STRING', NULL, false, true, NULL, 55, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000242 AND code = 'ATTR000055');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001046, 9000000000242, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000242 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001047, 9000000000242, 'ATTR000101', '应用类型', 'STRING', NULL, false, true, NULL, 101, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000242 AND code = 'ATTR000101');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001048, 9000000000242, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000242 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001049, 9000000000242, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000242 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001050, 9000000000242, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000242 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001051, 9000000000243, 'ATTR000032', '电阻值', 'STRING', NULL, false, true, NULL, 32, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000243 AND code = 'ATTR000032');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001052, 9000000000243, 'ATTR000055', '功率', 'STRING', NULL, false, true, NULL, 55, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000243 AND code = 'ATTR000055');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001053, 9000000000243, 'ATTR000071', '冷却方式', 'STRING', NULL, false, true, NULL, 71, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000243 AND code = 'ATTR000071');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001054, 9000000000243, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000243 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001055, 9000000000243, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000243 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001056, 9000000000243, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000243 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001057, 9000000000244, 'ATTR000011', '材质', 'STRING', NULL, false, true, NULL, 11, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000244 AND code = 'ATTR000011');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001058, 9000000000244, 'ATTR000043', '防护等级', 'STRING', NULL, false, true, NULL, 43, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000244 AND code = 'ATTR000043');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001059, 9000000000244, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000244 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001060, 9000000000244, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000244 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001061, 9000000000244, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000244 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001062, 9000000000245, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000245 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001063, 9000000000245, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000245 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001064, 9000000000245, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000245 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001065, 9000000000246, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000246 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001066, 9000000000246, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000246 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001067, 9000000000246, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000246 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001068, 9000000000246, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000246 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001069, 9000000000247, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000247 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001070, 9000000000247, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000247 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001071, 9000000000247, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000247 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001072, 9000000000247, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000247 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001073, 9000000000248, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000248 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001074, 9000000000248, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000248 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001075, 9000000000248, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000248 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001076, 9000000000248, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000248 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001077, 9000000000249, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000249 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001078, 9000000000249, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000249 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001079, 9000000000249, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000249 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001080, 9000000000249, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000249 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001081, 9000000000250, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000250 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001082, 9000000000250, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000250 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001083, 9000000000250, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000250 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001084, 9000000000250, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000250 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001085, 9000000000251, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000251 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001086, 9000000000251, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000251 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001087, 9000000000251, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000251 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001088, 9000000000251, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000251 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001089, 9000000000252, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000252 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001090, 9000000000252, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000252 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001091, 9000000000252, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000252 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001092, 9000000000252, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000252 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001093, 9000000000253, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000253 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001094, 9000000000253, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000253 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001095, 9000000000253, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000253 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001096, 9000000000253, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000253 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001097, 9000000000254, 'ATTR000011', '材质', 'STRING', NULL, false, true, NULL, 11, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000254 AND code = 'ATTR000011');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001098, 9000000000254, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000254 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001099, 9000000000254, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000254 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001100, 9000000000254, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000254 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001101, 9000000000254, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000254 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001102, 9000000000255, 'ATTR000011', '材质', 'STRING', NULL, false, true, NULL, 11, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000255 AND code = 'ATTR000011');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001103, 9000000000255, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000255 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001104, 9000000000255, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000255 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001105, 9000000000255, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000255 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001106, 9000000000255, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000255 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001107, 9000000000256, 'ATTR000011', '材质', 'STRING', NULL, false, true, NULL, 11, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000256 AND code = 'ATTR000011');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001108, 9000000000256, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000256 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001109, 9000000000256, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000256 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001110, 9000000000256, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000256 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001111, 9000000000256, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000256 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001112, 9000000000257, 'ATTR000011', '材质', 'STRING', NULL, false, true, NULL, 11, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000257 AND code = 'ATTR000011');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001113, 9000000000257, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000257 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001114, 9000000000257, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000257 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001115, 9000000000257, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000257 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001116, 9000000000257, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000257 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001117, 9000000000258, 'ATTR000011', '材质', 'STRING', NULL, false, true, NULL, 11, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000258 AND code = 'ATTR000011');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001118, 9000000000258, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000258 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001119, 9000000000258, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000258 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001120, 9000000000258, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000258 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001121, 9000000000258, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000258 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001122, 9000000000259, 'ATTR000011', '材质', 'STRING', NULL, false, true, NULL, 11, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000259 AND code = 'ATTR000011');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001123, 9000000000259, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000259 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001124, 9000000000259, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000259 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001125, 9000000000259, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000259 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001126, 9000000000259, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000259 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001127, 9000000000260, 'ATTR000011', '材质', 'STRING', NULL, false, true, NULL, 11, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000260 AND code = 'ATTR000011');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001128, 9000000000260, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000260 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001129, 9000000000260, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000260 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001130, 9000000000260, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000260 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001131, 9000000000260, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000260 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001132, 9000000000261, 'ATTR000011', '材质', 'STRING', NULL, false, true, NULL, 11, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000261 AND code = 'ATTR000011');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001133, 9000000000261, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000261 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001134, 9000000000261, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000261 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001135, 9000000000261, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000261 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001136, 9000000000261, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000261 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001137, 9000000000262, 'ATTR000011', '材质', 'STRING', NULL, false, true, NULL, 11, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000262 AND code = 'ATTR000011');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001138, 9000000000262, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000262 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001139, 9000000000262, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000262 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001140, 9000000000262, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000262 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001141, 9000000000262, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000262 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001142, 9000000000263, 'ATTR000011', '材质', 'STRING', NULL, false, true, NULL, 11, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000263 AND code = 'ATTR000011');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001143, 9000000000263, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000263 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001144, 9000000000263, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000263 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001145, 9000000000263, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000263 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001146, 9000000000263, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000263 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001147, 9000000000264, 'ATTR000011', '材质', 'STRING', NULL, false, true, NULL, 11, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000264 AND code = 'ATTR000011');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001148, 9000000000264, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000264 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001149, 9000000000264, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000264 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001150, 9000000000264, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000264 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001151, 9000000000264, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000264 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001152, 9000000000265, 'ATTR000011', '材质', 'STRING', NULL, false, true, NULL, 11, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000265 AND code = 'ATTR000011');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001153, 9000000000265, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000265 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001154, 9000000000265, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000265 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001155, 9000000000265, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000265 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001156, 9000000000265, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000265 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001157, 9000000000266, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000266 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001158, 9000000000266, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000266 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001159, 9000000000266, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000266 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001160, 9000000000266, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000266 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001161, 9000000000267, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000267 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001162, 9000000000267, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000267 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001163, 9000000000267, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000267 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001164, 9000000000267, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000267 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001165, 9000000000268, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000268 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001166, 9000000000268, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000268 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001167, 9000000000268, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000268 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001168, 9000000000268, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000268 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001169, 9000000000269, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000269 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001170, 9000000000269, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000269 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001171, 9000000000269, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000269 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001172, 9000000000269, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000269 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001173, 9000000000270, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000270 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001174, 9000000000270, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000270 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001175, 9000000000270, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000270 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001176, 9000000000270, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000270 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001177, 9000000000271, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000271 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001178, 9000000000271, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000271 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001179, 9000000000271, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000271 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001180, 9000000000271, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000271 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001181, 9000000000272, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000272 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001182, 9000000000272, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000272 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001183, 9000000000272, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000272 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001184, 9000000000272, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000272 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001185, 9000000000273, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000273 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001186, 9000000000273, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000273 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001187, 9000000000273, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000273 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001188, 9000000000273, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000273 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001189, 9000000000274, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000274 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001190, 9000000000274, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000274 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001191, 9000000000274, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000274 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001192, 9000000000275, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000275 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001193, 9000000000275, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000275 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001194, 9000000000275, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000275 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001195, 9000000000276, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000276 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001196, 9000000000276, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000276 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001197, 9000000000276, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000276 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001198, 9000000000277, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000277 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001199, 9000000000277, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000277 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001200, 9000000000277, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000277 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001201, 9000000000278, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000278 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001202, 9000000000278, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000278 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001203, 9000000000278, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000278 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001204, 9000000000279, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000279 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001205, 9000000000279, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000279 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001206, 9000000000279, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000279 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001207, 9000000000280, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000280 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001208, 9000000000280, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000280 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001209, 9000000000280, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000280 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001210, 9000000000281, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000281 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001211, 9000000000281, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000281 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001212, 9000000000281, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000281 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001213, 9000000000282, 'ATTR000012', '参数', 'STRING', NULL, false, true, NULL, 12, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000282 AND code = 'ATTR000012');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001214, 9000000000282, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000282 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001215, 9000000000282, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000282 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001216, 9000000000283, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000283 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001217, 9000000000283, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000283 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001218, 9000000000283, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000283 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001219, 9000000000284, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000284 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001220, 9000000000284, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000284 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001221, 9000000000284, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000284 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001222, 9000000000284, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000284 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001223, 9000000000285, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000285 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001224, 9000000000285, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000285 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001225, 9000000000285, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000285 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001226, 9000000000285, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000285 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001227, 9000000000286, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000286 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001228, 9000000000286, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000286 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001229, 9000000000286, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000286 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001230, 9000000000286, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000286 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001231, 9000000000287, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000287 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001232, 9000000000287, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000287 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001233, 9000000000287, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000287 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001234, 9000000000288, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000288 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001235, 9000000000288, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000288 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001236, 9000000000288, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000288 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001237, 9000000000288, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000288 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001238, 9000000000289, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000289 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001239, 9000000000289, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000289 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001240, 9000000000289, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000289 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001241, 9000000000290, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000290 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001242, 9000000000290, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000290 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001243, 9000000000290, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000290 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001244, 9000000000290, 'ATTR000113', '订货号', 'STRING', NULL, false, true, NULL, 113, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000290 AND code = 'ATTR000113');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001245, 9000000000291, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000291 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001246, 9000000000291, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000291 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001247, 9000000000291, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000291 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001248, 9000000000292, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000292 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001249, 9000000000292, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000292 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001250, 9000000000292, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000292 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001251, 9000000000293, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000293 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001252, 9000000000293, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000293 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001253, 9000000000293, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000293 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001254, 9000000000294, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000294 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001255, 9000000000294, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000294 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001256, 9000000000294, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000294 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001257, 9000000000295, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000295 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001258, 9000000000295, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000295 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001259, 9000000000295, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000295 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001260, 9000000000296, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000296 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001261, 9000000000296, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000296 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001262, 9000000000296, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000296 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001263, 9000000000297, 'ATTR000076', '品牌', 'STRING', NULL, false, true, NULL, 76, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000297 AND code = 'ATTR000076');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001264, 9000000000297, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000297 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001265, 9000000000297, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000297 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001266, 9000000000298, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000298 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001267, 9000000000298, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000298 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001268, 9000000000299, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000299 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001269, 9000000000299, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000299 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001270, 9000000000300, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000300 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001271, 9000000000300, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000300 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001272, 9000000000300, 'ATTR000116', '类别名称', 'STRING', NULL, false, true, NULL, 116, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000300 AND code = 'ATTR000116');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001273, 9000000000300, 'ATTR000118', '类型', 'STRING', NULL, false, true, NULL, 118, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000300 AND code = 'ATTR000118');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001274, 9000000000301, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000301 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001275, 9000000000301, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000301 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001276, 9000000000302, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000302 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001277, 9000000000302, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000302 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001278, 9000000000303, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000303 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001279, 9000000000303, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000303 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001280, 9000000000304, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000304 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001281, 9000000000304, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000304 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001282, 9000000000305, 'ATTR000111', '附加说明', 'STRING', NULL, false, true, NULL, 111, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000305 AND code = 'ATTR000111');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001283, 9000000000305, 'ATTR000112', '物料是否需要认证', 'STRING', NULL, false, true, NULL, 112, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000305 AND code = 'ATTR000112');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001284, 9000000000305, 'ATTR000116', '类别名称', 'STRING', NULL, false, true, NULL, 116, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000305 AND code = 'ATTR000116');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001285, 9000000000305, 'ATTR000117', '外围设备', 'STRING', NULL, false, true, NULL, 117, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000305 AND code = 'ATTR000117');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001286, 9000000000305, 'ATTR000119', '线缆', 'STRING', NULL, false, true, NULL, 119, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000305 AND code = 'ATTR000119');
INSERT INTO doc_class_attr (id, class_id, code, name, value_type, enum_code, required, inheritable, default_value, sort, deleted, created_at, updated_at)
SELECT 9100000001287, 9000000000305, 'ATTR000120', '配件', 'STRING', NULL, false, true, NULL, 120, false, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class_attr WHERE class_id = 9000000000305 AND code = 'ATTR000120');

COMMIT;

-- ---------------------------------------------------------------------
-- 待确认：蓝本 ISEMPTY=1 的属性（7 条口径未定，暂按 required=false）
-- 若确认「ISEMPTY=1 表示允许为空」，则当前设置正确，无需改动；
-- 若确认「ISEMPTY=0 表示可空」，需把 ISEMPTY=0 的那批改为 required=true（谨慎：会影响录入）。
-- ---------------------------------------------------------------------
--   ISEMPTY=1: 000114 剩余电流（分类 100106）
--   ISEMPTY=1: 000115 防爆等级（分类 101902）
--   ISEMPTY=1: 000116 类别名称（分类 120403）
--   ISEMPTY=1: 000118 类型（分类 120403）
--   ISEMPTY=1: 000116 类别名称（分类 120408）
--   ISEMPTY=1: 000117 外围设备（分类 120408）
--   ISEMPTY=1: 000119 线缆（分类 120408）
--   ISEMPTY=1: 000120 配件（分类 120408）
