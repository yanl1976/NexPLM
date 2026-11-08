-- =====================================================================
-- 零部件分类迁移：蓝本 CLASSIFYLINK → 新系统 doc_class
-- 生成器：tools/Gen-DocClassMigration.ps1（只读蓝本，仅生成 SQL，不执行）
-- 数据源：198.168.6.26,1433/TCPLM；生成时间：2026-11-08 15:06:31
-- 统计：节点 305 个（根 4 + 分组 37 + 分类 264），最大深度 4
-- 口径：entity_code=PdmItem；path 形如 /父编码/子编码；幂等（同 code 已存在则跳过）
-- 注意：执行前需确认目标库写权限与业务窗口（A3 迁移演练）；本文件未包含分类属性迁移。
-- =====================================================================

BEGIN;

INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000001, NULL, 'PdmItem', '80', '成品库', '/80', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '80');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000002, NULL, 'PdmItem', 'CH', '产号', '/CH', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = 'CH');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000003, NULL, 'PdmItem', 'LBZJ', '零部组件', '/LBZJ', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = 'LBZJ');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000004, NULL, 'PdmItem', 'QGD', '请购单', '/QGD', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = 'QGD');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000005, 9000000000001, 'PdmItem', '8010', '钻采产品', '/80/8010', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '8010');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000006, 9000000000001, 'PdmItem', '8011', '自动化', '/80/8011', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '8011');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000007, 9000000000001, 'PdmItem', '8012', '电源', '/80/8012', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '8012');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000008, 9000000000001, 'PdmItem', '8013', '电能质量', '/80/8013', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '8013');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000009, 9000000000001, 'PdmItem', '8014', '新能源', '/80/8014', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '8014');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000010, 9000000000001, 'PdmItem', '8015', '变频器', '/80/8015', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '8015');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000011, 9000000000002, 'PdmItem', 'CH001', '产号', '/CH/CH001', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = 'CH001');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000012, 9000000000003, 'PdmItem', '10', '电气元件', '/LBZJ/10', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '10');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000013, 9000000000003, 'PdmItem', '11', '原材料', '/LBZJ/11', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '11');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000014, 9000000000003, 'PdmItem', '12', '外购标准件', '/LBZJ/12', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '12');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000015, 9000000000003, 'PdmItem', '2001', '非标自制件', '/LBZJ/2001', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '2001');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000016, 9000000000003, 'PdmItem', '2002', '通用自制件', '/LBZJ/2002', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '2002');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000017, 9000000000004, 'PdmItem', 'DYCPSYB', '电源产品事业部', '/QGD/DYCPSYB', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = 'DYCPSYB');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000018, 9000000000004, 'PdmItem', 'YFB', '研发部', '/QGD/YFB', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = 'YFB');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000019, 9000000000004, 'PdmItem', 'ZDHGS', '自动化工程公司', '/QGD/ZDHGS', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = 'ZDHGS');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000020, 9000000000005, 'PdmItem', '801001', '直流钻机电控系统', '/80/8010/801001', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801001');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000021, 9000000000005, 'PdmItem', '801002', '交流钻机电控系统', '/80/8010/801002', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801002');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000022, 9000000000005, 'PdmItem', '801003', '直流网电系统', '/80/8010/801003', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801003');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000023, 9000000000005, 'PdmItem', '801004', '交流网电系统', '/80/8010/801004', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801004');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000024, 9000000000005, 'PdmItem', '801005', '顶驱电控系统', '/80/8010/801005', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801005');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000025, 9000000000005, 'PdmItem', '801006', '电缆转接房', '/80/8010/801006', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801006');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000026, 9000000000005, 'PdmItem', '801007', '并机柜', '/80/8010/801007', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801007');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000027, 9000000000005, 'PdmItem', '801008', '并机房', '/80/8010/801008', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801008');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000028, 9000000000005, 'PdmItem', '801009', '电源切换柜', '/80/8010/801009', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801009');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000029, 9000000000005, 'PdmItem', '801010', 'IE-house', '/80/8010/801010', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801010');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000030, 9000000000005, 'PdmItem', '801011', '吊环保护装置', '/80/8010/801011', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801011');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000031, 9000000000005, 'PdmItem', '801012', '码头岸电与船舶电力推进系统', '/80/8010/801012', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801012');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000032, 9000000000005, 'PdmItem', '801013', '变频电源装置', '/80/8010/801013', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801013');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000033, 9000000000005, 'PdmItem', '801014', '司钻房', '/80/8010/801014', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801014');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000034, 9000000000005, 'PdmItem', '801015', '开关柜', '/80/8010/801015', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801015');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000035, 9000000000005, 'PdmItem', '801016', '发电柜', '/80/8010/801016', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801016');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000036, 9000000000005, 'PdmItem', '801017', '综合柜', '/80/8010/801017', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801017');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000037, 9000000000005, 'PdmItem', '801018', '传动柜', '/80/8010/801018', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801018');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000038, 9000000000005, 'PdmItem', '801019', '整流柜', '/80/8010/801019', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801019');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000039, 9000000000005, 'PdmItem', '801020', '直流切换柜', '/80/8010/801020', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801020');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000040, 9000000000005, 'PdmItem', '801021', '软启柜', '/80/8010/801021', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801021');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000041, 9000000000005, 'PdmItem', '801022', '变频柜', '/80/8010/801022', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801022');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000042, 9000000000005, 'PdmItem', '801023', 'MCC柜', '/80/8010/801023', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801023');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000043, 9000000000005, 'PdmItem', '801024', '照明屏', '/80/8010/801024', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801024');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000044, 9000000000005, 'PdmItem', '801025', '励磁柜', '/80/8010/801025', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801025');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000045, 9000000000005, 'PdmItem', '801026', '送钻柜', '/80/8010/801026', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801026');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000046, 9000000000005, 'PdmItem', '801027', '司钻台（司泵台-顶驱箱-远控台-送钻台-绞车台）', '/80/8010/801027', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801027');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000047, 9000000000005, 'PdmItem', '801028', '其它', '/80/8010/801028', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801028');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000048, 9000000000005, 'PdmItem', '801029', '备件', '/80/8010/801029', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801029');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000049, 9000000000006, 'PdmItem', '801101', '自动化产品', '/80/8011/801101', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801101');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000050, 9000000000006, 'PdmItem', '801102', '矿井提升机电控产品', '/80/8011/801102', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801102');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000051, 9000000000006, 'PdmItem', '801103', '高低压配电产品', '/80/8011/801103', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801103');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000052, 9000000000006, 'PdmItem', '801104', '其它', '/80/8011/801104', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801104');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000053, 9000000000007, 'PdmItem', '801201', '加速器电源', '/80/8012/801201', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801201');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000054, 9000000000007, 'PdmItem', '801202', '医疗电源', '/80/8012/801202', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801202');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000055, 9000000000007, 'PdmItem', '801203', '其他', '/80/8012/801203', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801203');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000056, 9000000000008, 'PdmItem', '801301', '有源电力滤波器APF', '/80/8013/801301', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801301');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000057, 9000000000008, 'PdmItem', '801302', '电容柜', '/80/8013/801302', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801302');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000058, 9000000000008, 'PdmItem', '801303', '有源补偿装置ASVG', '/80/8013/801303', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801303');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000059, 9000000000008, 'PdmItem', '801304', 'SVG', '/80/8013/801304', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801304');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000060, 9000000000008, 'PdmItem', '801305', '其它', '/80/8013/801305', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801305');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000061, 9000000000009, 'PdmItem', '801401', '直流充电桩', '/80/8014/801401', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801401');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000062, 9000000000009, 'PdmItem', '801402', '交流充电桩', '/80/8014/801402', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801402');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000063, 9000000000009, 'PdmItem', '801403', '充电模块', '/80/8014/801403', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801403');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000064, 9000000000009, 'PdmItem', '801404', '并网柜', '/80/8014/801404', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801404');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000065, 9000000000009, 'PdmItem', '801405', '其它', '/80/8014/801405', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801405');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000066, 9000000000010, 'PdmItem', '801501', '进线柜', '/80/8015/801501', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801501');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000067, 9000000000010, 'PdmItem', '801502', '整流柜', '/80/8015/801502', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801502');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000068, 9000000000010, 'PdmItem', '801503', '逆变柜', '/80/8015/801503', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801503');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000069, 9000000000010, 'PdmItem', '801504', '其它', '/80/8015/801504', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '801504');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000070, 9000000000012, 'PdmItem', '1001', '断路器', '/LBZJ/10/1001', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '1001');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000071, 9000000000012, 'PdmItem', '1002', '接触器', '/LBZJ/10/1002', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '1002');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000072, 9000000000012, 'PdmItem', '1003', '刀开关', '/LBZJ/10/1003', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '1003');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000073, 9000000000012, 'PdmItem', '1004', '保护器', '/LBZJ/10/1004', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '1004');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000074, 9000000000012, 'PdmItem', '1005', '互感器/传感器', '/LBZJ/10/1005', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '1005');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000075, 9000000000012, 'PdmItem', '1006', '电抗器', '/LBZJ/10/1006', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '1006');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000076, 9000000000012, 'PdmItem', '1007', '变压器', '/LBZJ/10/1007', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '1007');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000077, 9000000000012, 'PdmItem', '1008', '继电器', '/LBZJ/10/1008', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '1008');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000078, 9000000000012, 'PdmItem', '1009', '仪器仪表', '/LBZJ/10/1009', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '1009');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000079, 9000000000012, 'PdmItem', '1010', '主令电器', '/LBZJ/10/1010', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '1010');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000080, 9000000000012, 'PdmItem', '1011', '信号器件', '/LBZJ/10/1011', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '1011');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000081, 9000000000012, 'PdmItem', '1012', '变送器', '/LBZJ/10/1012', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '1012');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000082, 9000000000012, 'PdmItem', '1013', '电阻', '/LBZJ/10/1013', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '1013');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000083, 9000000000012, 'PdmItem', '1014', '电容', '/LBZJ/10/1014', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '1014');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000084, 9000000000012, 'PdmItem', '1015', '电子器件', '/LBZJ/10/1015', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '1015');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000085, 9000000000012, 'PdmItem', '1016', '连接器件', '/LBZJ/10/1016', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '1016');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000086, 9000000000012, 'PdmItem', '1017', '绝缘件', '/LBZJ/10/1017', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '1017');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000087, 9000000000012, 'PdmItem', '1018', '其他器件', '/LBZJ/10/1018', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '1018');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000088, 9000000000012, 'PdmItem', '1019', '外购成套产品', '/LBZJ/10/1019', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '1019');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000089, 9000000000012, 'PdmItem', '1020', '程控器件', '/LBZJ/10/1020', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '1020');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000090, 9000000000013, 'PdmItem', '1101', '黑色金属', '/LBZJ/11/1101', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '1101');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000091, 9000000000013, 'PdmItem', '1102', '有色金属', '/LBZJ/11/1102', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '1102');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000092, 9000000000013, 'PdmItem', '1103', '电线;电缆', '/LBZJ/11/1103', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '1103');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000093, 9000000000013, 'PdmItem', '1104', '非金属材料', '/LBZJ/11/1104', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '1104');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000094, 9000000000014, 'PdmItem', '1201', '紧固件', '/LBZJ/12/1201', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '1201');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000095, 9000000000014, 'PdmItem', '1202', '杂品', '/LBZJ/12/1202', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '1202');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000096, 9000000000014, 'PdmItem', '1203', '抽屉', '/LBZJ/12/1203', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '1203');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000097, 9000000000014, 'PdmItem', '1204', '其它', '/LBZJ/12/1204', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '1204');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000098, 9000000000070, 'PdmItem', '100101', '固定式框架断路器', '/LBZJ/10/1001/100101', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100101');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000099, 9000000000070, 'PdmItem', '100102', '抽出式框架断路器', '/LBZJ/10/1001/100102', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100102');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000100, 9000000000070, 'PdmItem', '100103', '固定式塑壳断路器', '/LBZJ/10/1001/100103', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100103');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000101, 9000000000070, 'PdmItem', '100104', '抽出式塑壳断路器', '/LBZJ/10/1001/100104', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100104');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000102, 9000000000070, 'PdmItem', '100105', '微型断路器', '/LBZJ/10/1001/100105', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100105');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000103, 9000000000070, 'PdmItem', '100106', '漏电断路器', '/LBZJ/10/1001/100106', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100106');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000104, 9000000000070, 'PdmItem', '100107', '断路器附件', '/LBZJ/10/1001/100107', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100107');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000105, 9000000000070, 'PdmItem', '100108', '高压真空断路器', '/LBZJ/10/1001/100108', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100108');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000106, 9000000000071, 'PdmItem', '100201', '交流接触器', '/LBZJ/10/1002/100201', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100201');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000107, 9000000000071, 'PdmItem', '100202', '直流接触器', '/LBZJ/10/1002/100202', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100202');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000108, 9000000000071, 'PdmItem', '100203', '交流接触器线圈', '/LBZJ/10/1002/100203', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100203');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000109, 9000000000071, 'PdmItem', '100204', '接触器附件', '/LBZJ/10/1002/100204', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100204');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000110, 9000000000072, 'PdmItem', '100301', '刀熔开关', '/LBZJ/10/1003/100301', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100301');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000111, 9000000000072, 'PdmItem', '100302', '熔断器开关', '/LBZJ/10/1003/100302', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100302');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000112, 9000000000072, 'PdmItem', '100303', '单投刀开关', '/LBZJ/10/1003/100303', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100303');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000113, 9000000000072, 'PdmItem', '100304', '双投刀开关', '/LBZJ/10/1003/100304', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100304');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000114, 9000000000072, 'PdmItem', '100305', '低压负荷开关', '/LBZJ/10/1003/100305', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100305');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000115, 9000000000073, 'PdmItem', '100401', '熔断器', '/LBZJ/10/1004/100401', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100401');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000116, 9000000000073, 'PdmItem', '100402', '熔座', '/LBZJ/10/1004/100402', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100402');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000117, 9000000000073, 'PdmItem', '100403', '熔芯', '/LBZJ/10/1004/100403', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100403');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000118, 9000000000073, 'PdmItem', '100404', '避雷器', '/LBZJ/10/1004/100404', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100404');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000119, 9000000000073, 'PdmItem', '100405', '浪涌保护器', '/LBZJ/10/1004/100405', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100405');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000120, 9000000000073, 'PdmItem', '100406', '交直流转换器', '/LBZJ/10/1004/100406', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100406');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000121, 9000000000073, 'PdmItem', '100407', '适配器', '/LBZJ/10/1004/100407', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100407');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000122, 9000000000073, 'PdmItem', '100408', '其它', '/LBZJ/10/1004/100408', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100408');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000123, 9000000000074, 'PdmItem', '100501', '电流互感器', '/LBZJ/10/1005/100501', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100501');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000124, 9000000000074, 'PdmItem', '100502', '电压互感器', '/LBZJ/10/1005/100502', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100502');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000125, 9000000000074, 'PdmItem', '100503', '零序电流互感器', '/LBZJ/10/1005/100503', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100503');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000126, 9000000000074, 'PdmItem', '100504', '分流器', '/LBZJ/10/1005/100504', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100504');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000127, 9000000000074, 'PdmItem', '100505', '霍尔元件', '/LBZJ/10/1005/100505', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100505');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000128, 9000000000074, 'PdmItem', '100506', '热电偶', '/LBZJ/10/1005/100506', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100506');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000129, 9000000000074, 'PdmItem', '100507', '电流传感器', '/LBZJ/10/1005/100507', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100507');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000130, 9000000000074, 'PdmItem', '100508', '电压传感器', '/LBZJ/10/1005/100508', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100508');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000131, 9000000000074, 'PdmItem', '100509', '压力传感器', '/LBZJ/10/1005/100509', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100509');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000132, 9000000000074, 'PdmItem', '100510', '温湿度传感器', '/LBZJ/10/1005/100510', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100510');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000133, 9000000000075, 'PdmItem', '100601', '三相电抗器', '/LBZJ/10/1006/100601', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100601');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000134, 9000000000075, 'PdmItem', '100602', '单相电抗器', '/LBZJ/10/1006/100602', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100602');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000135, 9000000000076, 'PdmItem', '100701', '三相变压器', '/LBZJ/10/1007/100701', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100701');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000136, 9000000000076, 'PdmItem', '100702', '单相变压器', '/LBZJ/10/1007/100702', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100702');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000137, 9000000000076, 'PdmItem', '100703', '脉冲变压器', '/LBZJ/10/1007/100703', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100703');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000138, 9000000000076, 'PdmItem', '100704', '其他变压器', '/LBZJ/10/1007/100704', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100704');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000139, 9000000000077, 'PdmItem', '100801', '时间继电器', '/LBZJ/10/1008/100801', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100801');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000140, 9000000000077, 'PdmItem', '100802', '中间继电器', '/LBZJ/10/1008/100802', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100802');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000141, 9000000000077, 'PdmItem', '100803', '保护继电器', '/LBZJ/10/1008/100803', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100803');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000142, 9000000000077, 'PdmItem', '100804', '继电器座', '/LBZJ/10/1008/100804', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100804');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000143, 9000000000077, 'PdmItem', '100805', '热继电器', '/LBZJ/10/1008/100805', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100805');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000144, 9000000000078, 'PdmItem', '100901', '电压表', '/LBZJ/10/1009/100901', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100901');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000145, 9000000000078, 'PdmItem', '100902', '电流表', '/LBZJ/10/1009/100902', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100902');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000146, 9000000000078, 'PdmItem', '100903', '功率表', '/LBZJ/10/1009/100903', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100903');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000147, 9000000000078, 'PdmItem', '100904', '频率表', '/LBZJ/10/1009/100904', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100904');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000148, 9000000000078, 'PdmItem', '100905', '功率因数表', '/LBZJ/10/1009/100905', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100905');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000149, 9000000000078, 'PdmItem', '100906', '同期表', '/LBZJ/10/1009/100906', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100906');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000150, 9000000000078, 'PdmItem', '100907', '电度表', '/LBZJ/10/1009/100907', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100907');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000151, 9000000000078, 'PdmItem', '100908', '绝缘检测表', '/LBZJ/10/1009/100908', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100908');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000152, 9000000000078, 'PdmItem', '100909', '编码器', '/LBZJ/10/1009/100909', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100909');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000153, 9000000000078, 'PdmItem', '100910', '多功能仪表', '/LBZJ/10/1009/100910', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100910');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000154, 9000000000078, 'PdmItem', '100911', '压力表', '/LBZJ/10/1009/100911', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100911');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000155, 9000000000078, 'PdmItem', '100912', '随机仪器', '/LBZJ/10/1009/100912', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100912');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000156, 9000000000078, 'PdmItem', '100913', '仿真器', '/LBZJ/10/1009/100913', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100913');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000157, 9000000000078, 'PdmItem', '100914', '测温仪', '/LBZJ/10/1009/100914', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '100914');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000158, 9000000000079, 'PdmItem', '101001', '按钮', '/LBZJ/10/1010/101001', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101001');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000159, 9000000000079, 'PdmItem', '101002', '带灯按钮', '/LBZJ/10/1010/101002', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101002');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000160, 9000000000079, 'PdmItem', '101003', '转换开关', '/LBZJ/10/1010/101003', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101003');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000161, 9000000000079, 'PdmItem', '101004', '特殊开关', '/LBZJ/10/1010/101004', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101004');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000162, 9000000000079, 'PdmItem', '101005', '钮子开关', '/LBZJ/10/1010/101005', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101005');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000163, 9000000000079, 'PdmItem', '101006', '微动开关', '/LBZJ/10/1010/101006', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101006');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000164, 9000000000079, 'PdmItem', '101007', '温度开关', '/LBZJ/10/1010/101007', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101007');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000165, 9000000000079, 'PdmItem', '101008', '行程开关', '/LBZJ/10/1010/101008', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101008');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000166, 9000000000079, 'PdmItem', '101009', '磁开关', '/LBZJ/10/1010/101009', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101009');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000167, 9000000000079, 'PdmItem', '101010', '附件', '/LBZJ/10/1010/101010', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101010');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000168, 9000000000080, 'PdmItem', '101101', '指示灯', '/LBZJ/10/1011/101101', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101101');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000169, 9000000000080, 'PdmItem', '101102', '电子牌', '/LBZJ/10/1011/101102', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101102');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000170, 9000000000080, 'PdmItem', '101103', '报警器件', '/LBZJ/10/1011/101103', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101103');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000171, 9000000000081, 'PdmItem', '101201', '电压变送器', '/LBZJ/10/1012/101201', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101201');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000172, 9000000000081, 'PdmItem', '101202', '电流变送器', '/LBZJ/10/1012/101202', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101202');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000173, 9000000000081, 'PdmItem', '101203', '综合变送器', '/LBZJ/10/1012/101203', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101203');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000174, 9000000000081, 'PdmItem', '101204', '功率变送器', '/LBZJ/10/1012/101204', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101204');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000175, 9000000000081, 'PdmItem', '101205', '频率变送器', '/LBZJ/10/1012/101205', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101205');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000176, 9000000000081, 'PdmItem', '101206', '隔离器', '/LBZJ/10/1012/101206', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101206');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000177, 9000000000082, 'PdmItem', '101301', '普通电阻', '/LBZJ/10/1013/101301', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101301');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000178, 9000000000082, 'PdmItem', '101302', '精密电阻', '/LBZJ/10/1013/101302', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101302');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000179, 9000000000082, 'PdmItem', '101303', '可调电阻', '/LBZJ/10/1013/101303', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101303');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000180, 9000000000082, 'PdmItem', '101304', '压敏电阻', '/LBZJ/10/1013/101304', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101304');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000181, 9000000000082, 'PdmItem', '101305', '热敏电阻', '/LBZJ/10/1013/101305', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101305');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000182, 9000000000082, 'PdmItem', '101306', '管型电阻', '/LBZJ/10/1013/101306', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101306');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000183, 9000000000082, 'PdmItem', '101307', '铝壳电阻', '/LBZJ/10/1013/101307', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101307');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000184, 9000000000082, 'PdmItem', '101308', '电位器', '/LBZJ/10/1013/101308', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101308');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000185, 9000000000083, 'PdmItem', '101401', '铝电解电容（CD）', '/LBZJ/10/1014/101401', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101401');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000186, 9000000000083, 'PdmItem', '101402', '聚酯膜电容（CL）', '/LBZJ/10/1014/101402', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101402');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000187, 9000000000083, 'PdmItem', '101403', '聚丙烯膜电容（CBB）', '/LBZJ/10/1014/101403', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101403');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000188, 9000000000083, 'PdmItem', '101404', '鉏电解电容（CACQ）', '/LBZJ/10/1014/101404', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101404');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000189, 9000000000083, 'PdmItem', '101405', '金属化纸介电容（CJ）', '/LBZJ/10/1014/101405', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101405');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000190, 9000000000083, 'PdmItem', '101406', '复合介质电容（CHCZ）', '/LBZJ/10/1014/101406', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101406');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000191, 9000000000083, 'PdmItem', '101407', '瓷片电容（CT）', '/LBZJ/10/1014/101407', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101407');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000192, 9000000000083, 'PdmItem', '101408', '突波吸收电容', '/LBZJ/10/1014/101408', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101408');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000193, 9000000000083, 'PdmItem', '101409', 'RS库号电容', '/LBZJ/10/1014/101409', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101409');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000194, 9000000000083, 'PdmItem', '101410', '其它类型电容', '/LBZJ/10/1014/101410', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101410');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000195, 9000000000083, 'PdmItem', '101411', '薄膜电容', '/LBZJ/10/1014/101411', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101411');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000196, 9000000000084, 'PdmItem', '101501', '二极管', '/LBZJ/10/1015/101501', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101501');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000197, 9000000000084, 'PdmItem', '101502', '三极管', '/LBZJ/10/1015/101502', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101502');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000198, 9000000000084, 'PdmItem', '101503', '场效应管', '/LBZJ/10/1015/101503', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101503');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000199, 9000000000084, 'PdmItem', '101504', '晶闸管', '/LBZJ/10/1015/101504', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101504');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000200, 9000000000084, 'PdmItem', '101505', '电子管', '/LBZJ/10/1015/101505', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101505');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000201, 9000000000084, 'PdmItem', '101506', '其他器件', '/LBZJ/10/1015/101506', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101506');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000202, 9000000000084, 'PdmItem', '101507', '整流器件', '/LBZJ/10/1015/101507', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101507');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000203, 9000000000084, 'PdmItem', '101508', '充电机', '/LBZJ/10/1015/101508', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101508');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000204, 9000000000084, 'PdmItem', '101509', '电源模块', '/LBZJ/10/1015/101509', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101509');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000205, 9000000000084, 'PdmItem', '101510', '电源', '/LBZJ/10/1015/101510', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101510');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000206, 9000000000084, 'PdmItem', '101511', '集成电路', '/LBZJ/10/1015/101511', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101511');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000207, 9000000000084, 'PdmItem', '101512', '电力模块', '/LBZJ/10/1015/101512', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101512');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000208, 9000000000084, 'PdmItem', '101513', 'IGBT', '/LBZJ/10/1015/101513', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101513');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000209, 9000000000084, 'PdmItem', '101514', '滤波器', '/LBZJ/10/1015/101514', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101514');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000210, 9000000000084, 'PdmItem', '101515', '交换机', '/LBZJ/10/1015/101515', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101515');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000211, 9000000000084, 'PdmItem', '101516', 'RS产品', '/LBZJ/10/1015/101516', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101516');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000212, 9000000000085, 'PdmItem', '101601', '端子', '/LBZJ/10/1016/101601', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101601');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000213, 9000000000085, 'PdmItem', '101602', '一次插件', '/LBZJ/10/1016/101602', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101602');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000214, 9000000000085, 'PdmItem', '101603', '二次插件', '/LBZJ/10/1016/101603', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101603');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000215, 9000000000085, 'PdmItem', '101604', '插头', '/LBZJ/10/1016/101604', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101604');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000216, 9000000000085, 'PdmItem', '101605', '插座', '/LBZJ/10/1016/101605', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101605');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000217, 9000000000085, 'PdmItem', '101606', '彩色排线', '/LBZJ/10/1016/101606', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101606');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000218, 9000000000085, 'PdmItem', '101607', '接插件', '/LBZJ/10/1016/101607', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101607');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000219, 9000000000086, 'PdmItem', '101701', '母线框', '/LBZJ/10/1017/101701', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101701');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000220, 9000000000086, 'PdmItem', '101702', '母线夹', '/LBZJ/10/1017/101702', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101702');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000221, 9000000000086, 'PdmItem', '101703', '高压绝缘子', '/LBZJ/10/1017/101703', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101703');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000222, 9000000000086, 'PdmItem', '101704', '低压绝缘子', '/LBZJ/10/1017/101704', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101704');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000223, 9000000000086, 'PdmItem', '101705', '绝缘套管', '/LBZJ/10/1017/101705', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101705');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000224, 9000000000086, 'PdmItem', '101706', '热缩管', '/LBZJ/10/1017/101706', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101706');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000225, 9000000000087, 'PdmItem', '101801', '散热器/加热器', '/LBZJ/10/1018/101801', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101801');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000226, 9000000000087, 'PdmItem', '101802', '风机', '/LBZJ/10/1018/101802', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101802');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000227, 9000000000087, 'PdmItem', '101803', '磁环', '/LBZJ/10/1018/101803', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101803');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000228, 9000000000087, 'PdmItem', '101804', '电池', '/LBZJ/10/1018/101804', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101804');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000229, 9000000000087, 'PdmItem', '101805', '器件附件', '/LBZJ/10/1018/101805', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101805');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000230, 9000000000087, 'PdmItem', '101806', '电工器件', '/LBZJ/10/1018/101806', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101806');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000231, 9000000000087, 'PdmItem', '101807', '照明器具', '/LBZJ/10/1018/101807', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101807');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000232, 9000000000087, 'PdmItem', '101808', '其它器件', '/LBZJ/10/1018/101808', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101808');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000233, 9000000000087, 'PdmItem', '101809', '电源线', '/LBZJ/10/1018/101809', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101809');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000234, 9000000000087, 'PdmItem', '101810', '电机', '/LBZJ/10/1018/101810', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101810');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000235, 9000000000088, 'PdmItem', '101901', '变频柜', '/LBZJ/10/1019/101901', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101901');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000236, 9000000000088, 'PdmItem', '101902', '防爆箱', '/LBZJ/10/1019/101902', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101902');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000237, 9000000000088, 'PdmItem', '101903', '变频器', '/LBZJ/10/1019/101903', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101903');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000238, 9000000000088, 'PdmItem', '101904', '软启动器', '/LBZJ/10/1019/101904', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101904');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000239, 9000000000088, 'PdmItem', '101905', '制动单元', '/LBZJ/10/1019/101905', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101905');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000240, 9000000000088, 'PdmItem', '101906', '配电箱', '/LBZJ/10/1019/101906', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101906');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000241, 9000000000088, 'PdmItem', '101907', '控制柜', '/LBZJ/10/1019/101907', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101907');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000242, 9000000000088, 'PdmItem', '101908', '调速器', '/LBZJ/10/1019/101908', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101908');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000243, 9000000000088, 'PdmItem', '101909', '电阻箱', '/LBZJ/10/1019/101909', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101909');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000244, 9000000000088, 'PdmItem', '101911', '电池箱', '/LBZJ/10/1019/101911', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101911');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000245, 9000000000088, 'PdmItem', '101912', '其它', '/LBZJ/10/1019/101912', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101912');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000246, 9000000000088, 'PdmItem', '101913', '安防设备', '/LBZJ/10/1019/101913', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '101913');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000247, 9000000000089, 'PdmItem', '102001', 'PC类（是否为PLC?）', '/LBZJ/10/1020/102001', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '102001');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000248, 9000000000089, 'PdmItem', '102002', '工控机', '/LBZJ/10/1020/102002', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '102002');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000249, 9000000000089, 'PdmItem', '102003', '电器控制系统', '/LBZJ/10/1020/102003', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '102003');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000250, 9000000000089, 'PdmItem', '102004', '电子控制器', '/LBZJ/10/1020/102004', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '102004');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000251, 9000000000089, 'PdmItem', '102005', '触摸屏', '/LBZJ/10/1020/102005', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '102005');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000252, 9000000000089, 'PdmItem', '102006', '驱动类', '/LBZJ/10/1020/102006', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '102006');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000253, 9000000000089, 'PdmItem', '102007', '调节器', '/LBZJ/10/1020/102007', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '102007');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000254, 9000000000090, 'PdmItem', '110101', '板材', '/LBZJ/11/1101/110101', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '110101');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000255, 9000000000090, 'PdmItem', '110102', '管材', '/LBZJ/11/1101/110102', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '110102');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000256, 9000000000090, 'PdmItem', '110103', '棒材', '/LBZJ/11/1101/110103', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '110103');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000257, 9000000000090, 'PdmItem', '110104', '型材', '/LBZJ/11/1101/110104', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '110104');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000258, 9000000000090, 'PdmItem', '110105', '带材', '/LBZJ/11/1101/110105', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '110105');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000259, 9000000000091, 'PdmItem', '110201', '铜及其合金', '/LBZJ/11/1102/110201', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '110201');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000260, 9000000000091, 'PdmItem', '110202', '铝及其合金', '/LBZJ/11/1102/110202', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '110202');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000261, 9000000000091, 'PdmItem', '110203', '锌及其合金', '/LBZJ/11/1102/110203', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '110203');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000262, 9000000000091, 'PdmItem', '110204', '其他金属及合金', '/LBZJ/11/1102/110204', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '110204');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000263, 9000000000092, 'PdmItem', '110301', '电线', '/LBZJ/11/1103/110301', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '110301');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000264, 9000000000092, 'PdmItem', '110302', '电缆', '/LBZJ/11/1103/110302', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '110302');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000265, 9000000000092, 'PdmItem', '110303', '通讯线', '/LBZJ/11/1103/110303', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '110303');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000266, 9000000000093, 'PdmItem', '110401', '气体', '/LBZJ/11/1104/110401', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '110401');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000267, 9000000000093, 'PdmItem', '110402', '玻璃', '/LBZJ/11/1104/110402', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '110402');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000268, 9000000000093, 'PdmItem', '110403', '电工材料', '/LBZJ/11/1104/110403', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '110403');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000269, 9000000000093, 'PdmItem', '110404', '辅料（走线槽/扎带/吸盘等）', '/LBZJ/11/1104/110404', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '110404');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000270, 9000000000093, 'PdmItem', '110405', '化工试剂', '/LBZJ/11/1104/110405', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '110405');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000271, 9000000000093, 'PdmItem', '110406', '橡胶制品', '/LBZJ/11/1104/110406', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '110406');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000272, 9000000000093, 'PdmItem', '110407', '油漆', '/LBZJ/11/1104/110407', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '110407');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000273, 9000000000093, 'PdmItem', '110408', '柴油', '/LBZJ/11/1104/110408', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '110408');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000274, 9000000000094, 'PdmItem', '120102', '螺钉', '/LBZJ/12/1201/120102', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '120102');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000275, 9000000000094, 'PdmItem', '120103', '螺杆', '/LBZJ/12/1201/120103', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '120103');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000276, 9000000000094, 'PdmItem', '120104', '螺母', '/LBZJ/12/1201/120104', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '120104');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000277, 9000000000094, 'PdmItem', '120105', '平垫', '/LBZJ/12/1201/120105', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '120105');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000278, 9000000000094, 'PdmItem', '120106', '弹垫', '/LBZJ/12/1201/120106', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '120106');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000279, 9000000000094, 'PdmItem', '120108', '销', '/LBZJ/12/1201/120108', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '120108');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000280, 9000000000094, 'PdmItem', '120109', '柳钉', '/LBZJ/12/1201/120109', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '120109');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000281, 9000000000094, 'PdmItem', '120110', '自攻螺钉', '/LBZJ/12/1201/120110', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '120110');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000282, 9000000000094, 'PdmItem', '120111', '挡圈', '/LBZJ/12/1201/120111', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '120111');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000283, 9000000000095, 'PdmItem', '120201', '空柜（箱/台/屏）体', '/LBZJ/12/1202/120201', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '120201');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000284, 9000000000095, 'PdmItem', '120202', '五金（锁/铰链/弹簧等）', '/LBZJ/12/1202/120202', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '120202');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000285, 9000000000095, 'PdmItem', '120203', '端头', '/LBZJ/12/1202/120203', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '120203');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000286, 9000000000095, 'PdmItem', '120204', '焊接材料', '/LBZJ/12/1202/120204', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '120204');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000287, 9000000000095, 'PdmItem', '120205', '安装附件（导轨等）', '/LBZJ/12/1202/120205', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '120205');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000288, 9000000000095, 'PdmItem', '120206', '标准箱体', '/LBZJ/12/1202/120206', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '120206');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000289, 9000000000095, 'PdmItem', '120207', '非标柜体', '/LBZJ/12/1202/120207', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '120207');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000290, 9000000000095, 'PdmItem', '120208', '随机工具', '/LBZJ/12/1202/120208', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '120208');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000291, 9000000000096, 'PdmItem', '120301', '1/2单元抽屉', '/LBZJ/12/1203/120301', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '120301');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000292, 9000000000096, 'PdmItem', '120302', '一单元抽屉', '/LBZJ/12/1203/120302', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '120302');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000293, 9000000000096, 'PdmItem', '120303', '二单元抽屉', '/LBZJ/12/1203/120303', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '120303');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000294, 9000000000096, 'PdmItem', '120304', '三单元抽屉', '/LBZJ/12/1203/120304', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '120304');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000295, 9000000000096, 'PdmItem', '120305', '四单元抽屉', '/LBZJ/12/1203/120305', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '120305');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000296, 9000000000096, 'PdmItem', '120306', '抽屉附件', '/LBZJ/12/1203/120306', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '120306');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000297, 9000000000096, 'PdmItem', '120307', '其它', '/LBZJ/12/1203/120307', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '120307');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000298, 9000000000097, 'PdmItem', '120401', '系统软件', '/LBZJ/12/1204/120401', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '120401');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000299, 9000000000097, 'PdmItem', '120402', '服务费', '/LBZJ/12/1204/120402', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '120402');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000300, 9000000000097, 'PdmItem', '120403', '计算机', '/LBZJ/12/1204/120403', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '120403');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000301, 9000000000097, 'PdmItem', '120404', '设备', '/LBZJ/12/1204/120404', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '120404');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000302, 9000000000097, 'PdmItem', '120405', '其它', '/LBZJ/12/1204/120405', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '120405');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000303, 9000000000097, 'PdmItem', '120406', '空调', '/LBZJ/12/1204/120406', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '120406');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000304, 9000000000097, 'PdmItem', '120407', '安防设备', '/LBZJ/12/1204/120407', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '120407');
INSERT INTO doc_class (id, parent_id, entity_code, code, name, path, sort, status, deleted, version, created_at, updated_at)
SELECT 9000000000305, 9000000000097, 'PdmItem', '120408', '计算机配件', '/LBZJ/12/1204/120408', 0, 'ACTIVE', false, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM doc_class WHERE entity_code = 'PdmItem' AND code = '120408');

COMMIT;
