-- =====================================================================
-- 零部件**样例**迁移：蓝本 PSEITEM + PSEBASEFORM → plm_object + item_master
-- 生成器：tools/Gen-ItemMigration.ps1（只读蓝本，仅生成 SQL，不执行）
-- 数据源：198.168.6.26,1433/TCPLM；生成时间：2026-11-08 15:17:14
-- 统计：每类 15 条，共 139 条对象；分类命中 4 条；重复编码 2 条（已加后缀）
-- 按类型：CIS=15, GAUGE=15, MANUAL=15, OUTSOURCE=15, PART=16, PRODUCT=15, SCRAP=3, SERVICE=15, STANDARD=15, TOOL=15
-- 口径：
--   1) ITEMTYPE 11 类 → itemType（EXP-20261108-36 映射表）；未知类型归 PART
--   2) 规格型号取 PSEITEM.GGXH；材料取 PSEBASEFORM.MATERIAL；重量仅数字可转时落库
--   3) 分类归属为单分类口径（多分类取编码最小），class_id 取自 010-doc_class.sql
--   4) ORIGIN=2 → BUY，其余 → MAKE（方向待现场确认）
--   5) 幂等：每个对象/子表都带 NOT EXISTS，可重复执行
-- 注意：这是**样例**，不是全量；全量迁移留给 A3 窗口（改为 CSV + COPY）
-- =====================================================================

BEGIN;

INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000002, 'PdmItem', 9200000000002, '100100001', '交流接触器', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '100100001');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000003, 'PdmItem', 9200000000003, '10010100001', '断路器', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10010100001');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000004, 'PdmItem', 9200000000004, '10010100002', '断路器', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10010100002');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000005, 'PdmItem', 9200000000005, '10010100006', '断路器', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10010100006');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000006, 'PdmItem', 9200000000006, '10010100007', '断路器', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10010100007');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000007, 'PdmItem', 9200000000007, '10010100008', '断路器', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10010100008');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000008, 'PdmItem', 9200000000008, '10010100012', '断路器', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10010100012');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000009, 'PdmItem', 9200000000009, '10010100013', '断路器', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10010100013');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000010, 'PdmItem', 9200000000010, '10010100014', '断路器', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10010100014');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000011, 'PdmItem', 9200000000011, '10010100015', '断路器', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10010100015');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000012, 'PdmItem', 9200000000012, '10010100016', '断路器', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10010100016');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000013, 'PdmItem', 9200000000013, '10010100017', '断路器', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10010100017');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000014, 'PdmItem', 9200000000014, '10010100018', '断路器', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10010100018');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000015, 'PdmItem', 9200000000015, '10040800351', '端子标记', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10040800351');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000016, 'PdmItem', 9200000000016, '10180400054', '胶体蓄电池', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10180400054');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000017, 'PdmItem', 9200000000017, '15000100001', '废铜排', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '15000100001');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000018, 'PdmItem', 9200000000018, '15000100003', '废铜排', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '15000100003');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000019, 'PdmItem', 9200000000019, '15000100003-18', '废铜排', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '15000100003-18');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000020, 'PdmItem', 9200000000020, '110100001', '铜棒', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '110100001');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000021, 'PdmItem', 9200000000021, '11010100001', '304#不锈钢板', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '11010100001');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000022, 'PdmItem', 9200000000022, '11010100002', '304#不锈钢板', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '11010100002');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000023, 'PdmItem', 9200000000023, '11010100003', '冷轧钢板', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '11010100003');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000024, 'PdmItem', 9200000000024, '11010100004', '冷轧钢板', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '11010100004');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000025, 'PdmItem', 9200000000025, '11010100005', '冷轧钢板', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '11010100005');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000026, 'PdmItem', 9200000000026, '11010100006', '热轧钢板', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '11010100006');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000027, 'PdmItem', 9200000000027, '11010100007', '热轧钢板', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '11010100007');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000028, 'PdmItem', 9200000000028, '11010100008', '热轧钢板', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '11010100008');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000029, 'PdmItem', 9200000000029, '11010100009', '热轧钢板', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '11010100009');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000030, 'PdmItem', 9200000000030, '11010100010', '不锈钢板', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '11010100010');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000031, 'PdmItem', 9200000000031, '11010100011', '热轧钢板', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '11010100011');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000032, 'PdmItem', 9200000000032, '11010100012', '不锈钢板', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '11010100012');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000033, 'PdmItem', 9200000000033, '11010100013', '不锈钢板', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '11010100013');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000034, 'PdmItem', 9200000000034, '11010100014', '冷轧钢板', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '11010100014');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000035, 'PdmItem', 9200000000035, '13010000001', '光纤敷设', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '13010000001');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000036, 'PdmItem', 9200000000036, '13010000002', '通讯线缆敷设', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '13010000002');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000037, 'PdmItem', 9200000000037, '13010000003', '安装费', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '13010000003');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000038, 'PdmItem', 9200000000038, '13010000004', '包装运输等', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '13010000004');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000039, 'PdmItem', 9200000000039, '13010000005', '辅材人工', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '13010000005');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000040, 'PdmItem', 9200000000040, '13010000006', '包装', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '13010000006');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000041, 'PdmItem', 9200000000041, '13010000007', '辅材', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '13010000007');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000042, 'PdmItem', 9200000000042, '13010000008', 'MPP电力管', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '13010000008');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000043, 'PdmItem', 9200000000043, '13010000009', 'MPP电力管', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '13010000009');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000044, 'PdmItem', 9200000000044, '13010000010', '电缆头', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '13010000010');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000045, 'PdmItem', 9200000000045, '13010000011', '电缆穿井敷设', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '13010000011');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000046, 'PdmItem', 9200000000046, '13010000012', '张拉膜雨棚钢构安装', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '13010000012');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000047, 'PdmItem', 9200000000047, '13010000013', '基础开发及基础制作', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '13010000013');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000048, 'PdmItem', 9200000000048, '13010000014', '建安费', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '13010000014');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000049, 'PdmItem', 9200000000049, '13010000015', '管理费', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '13010000015');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000050, 'PdmItem', 9200000000050, '109S184117B', '摇架装配', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '109S184117B');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000051, 'PdmItem', 9200000000051, '11020300031', '接地系统', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '11020300031');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000052, 'PdmItem', 9200000000052, '12020501503', '脚踏开关', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12020501503');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000053, 'PdmItem', 9200000000053, '12040800003', '内六角头螺钉', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12040800003');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000054, 'PdmItem', 9200000000054, '12040800004', '不锈钢十字槽沉头螺钉', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12040800004');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000055, 'PdmItem', 9200000000055, '12040800005', '平垫圈', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12040800005');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000056, 'PdmItem', 9200000000056, '12040800006', '波形弹性垫圈', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12040800006');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000057, 'PdmItem', 9200000000057, '12040800007', '1型六角螺母', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12040800007');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000058, 'PdmItem', 9200000000058, '12040800007-57', '1型六角螺母', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12040800007-57');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000059, 'PdmItem', 9200000000059, '121200', '短路器安装', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '121200');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000060, 'PdmItem', 9200000000060, '123S228023MX', 'S2012135-2', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '123S228023MX');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000061, 'PdmItem', 9200000000061, '123S228023Z', 'S2012135-2', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '123S228023Z');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000062, 'PdmItem', 9200000000062, '133S228017MX', 'S201385', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '133S228017MX');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000063, 'PdmItem', 9200000000063, '133S228017Z', 'S201385', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '133S228017Z');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000064, 'PdmItem', 9200000000064, '143S228019MX', 'K2014-01', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '143S228019MX');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000065, 'PdmItem', 9200000000065, '0', '柜体框架', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '0');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000066, 'PdmItem', 9200000000066, '0CT.325.503', '晶闸管整流柜布局图', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '0CT.325.503');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000067, 'PdmItem', 9200000000067, '0CT.354.544.6', 'SCR-A-2晶闸管整流柜电路图', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '0CT.354.544.6');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000068, 'PdmItem', 9200000000068, '0CT.354.544.7', 'SCR-B-1晶闸管整流柜电路图', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '0CT.354.544.7');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000069, 'PdmItem', 9200000000069, '112aeq', 'weqwewqeaa', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '112aeq');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000070, 'PdmItem', 9200000000070, '121214wwe', '12321qweqwe', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '121214wwe');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000071, 'PdmItem', 9200000000071, '2', '2', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '2');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000072, 'PdmItem', 9200000000072, '200CT.354.002J', '电路原理图', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '200CT.354.002J');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000073, 'PdmItem', 9200000000073, '200CT310001J', '柜门开孔图', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '200CT310001J');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000074, 'PdmItem', 9200000000074, '200CT310002J', '前门板开孔', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '200CT310002J');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000075, 'PdmItem', 9200000000075, '200CT3107301', '传动柜门开孔', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '200CT3107301');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000076, 'PdmItem', 9200000000076, '200CT3107302', '传动柜门开孔', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '200CT3107302');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000077, 'PdmItem', 9200000000077, '200CT3107311-3', '柜门开孔', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '200CT3107311-3');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000078, 'PdmItem', 9200000000078, '200CT3107341-3', '柜门开孔', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '200CT3107341-3');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000079, 'PdmItem', 9200000000079, '200CT310737', '散热器开孔图', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '200CT310737');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000080, 'PdmItem', 9200000000080, '200CT.354.561', '1200kW制动单元原理图', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '200CT.354.561');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000081, 'PdmItem', 9200000000081, '2010S100200Z', '500KW光伏逆变柜总装图', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '2010S100200Z');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000082, 'PdmItem', 9200000000082, '20150723', '备件', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '20150723');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000083, 'PdmItem', 9200000000083, '20150727', '备件', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '20150727');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000084, 'PdmItem', 9200000000084, '20150728', '备件1', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '20150728');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000085, 'PdmItem', 9200000000085, '2017S080001DT', '7kW交流桩原理图', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '2017S080001DT');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000086, 'PdmItem', 9200000000086, '2017S080001J', '7kW交流桩接线图', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '2017S080001J');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000087, 'PdmItem', 9200000000087, '2017S116100TM', '图样目录', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '2017S116100TM');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000088, 'PdmItem', 9200000000088, '2017S120100Z', '总装图', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '2017S120100Z');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000089, 'PdmItem', 9200000000089, '2017S120-200Z', '总装图', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '2017S120-200Z');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000090, 'PdmItem', 9200000000090, '2017S123100TM', '1100kW整流器', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '2017S123100TM');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000091, 'PdmItem', 9200000000091, '2017S125100DT', '钻机专用有源滤波柜原理图', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '2017S125100DT');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000092, 'PdmItem', 9200000000092, '2017S125100J1', '接线图', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '2017S125100J1');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000093, 'PdmItem', 9200000000093, '2017S125100J2', '接线图', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '2017S125100J2');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000094, 'PdmItem', 9200000000094, '2017S125100J3', '接线图', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '2017S125100J3');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000095, 'PdmItem', 9200000000095, 'QGD1月23日功能调整项目01', '干式变压器请购单', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = 'QGD1月23日功能调整项目01');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000096, 'PdmItem', 9200000000096, 'QGD1月23日功能调整项目02', 'CBC4', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = 'QGD1月23日功能调整项目02');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000097, 'PdmItem', 9200000000097, 'QGDB202206601', '请购单', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = 'QGDB202206601');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000098, 'PdmItem', 9200000000098, 'QGDB202206602', '111', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = 'QGDB202206602');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000099, 'PdmItem', 9200000000099, 'QGDB202206603', '11', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = 'QGDB202206603');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000100, 'PdmItem', 9200000000100, 'QGDB202310501', '甘肃省高速公路兰州北等58处服务区充电桩设备更新改造项目(项目)-箱变', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = 'QGDB202310501');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000101, 'PdmItem', 9200000000101, 'QGDB202310503', '箱变请购单', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = 'QGDB202310503');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000102, 'PdmItem', 9200000000102, 'QGDB202310504', '秦州服务区箱变维修', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = 'QGDB202310504');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000103, 'PdmItem', 9200000000103, 'QGDB20231551', '四川页岩气项目备件', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = 'QGDB20231551');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000104, 'PdmItem', 9200000000104, 'QGDB20231631', 'B2023163请购单', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = 'QGDB20231631');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000105, 'PdmItem', 9200000000105, 'QGDB202316401', '请购单', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = 'QGDB202316401');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000106, 'PdmItem', 9200000000106, 'QGDB202316601', '渤钻第一钻井分公司配件', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = 'QGDB202316601');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000107, 'PdmItem', 9200000000107, 'QGDB202316602', '电缆型号重提', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = 'QGDB202316602');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000108, 'PdmItem', 9200000000108, 'QGDB202316701', '渤海钻探第三钻井分公司', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = 'QGDB202316701');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000109, 'PdmItem', 9200000000109, 'QGDB202316702', '补提分压开关', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = 'QGDB202316702');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000110, 'PdmItem', 9200000000110, '00001', '自封口塑料袋', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '00001');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000111, 'PdmItem', 9200000000111, '00002', '自封口塑料袋', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '00002');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000112, 'PdmItem', 9200000000112, '00003', '套筒过渡头', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '00003');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000113, 'PdmItem', 9200000000113, '00005', '国重实验室研发平台司钻控制房', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '00005');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000114, 'PdmItem', 9200000000114, '00006', '防爆高清监控系统', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '00006');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000115, 'PdmItem', 9200000000115, '00007', '内部对讲系统', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '00007');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000116, 'PdmItem', 9200000000116, '00012', '直流充电枪', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '00012');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000117, 'PdmItem', 9200000000117, '00013', '充电桩前门', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '00013');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000118, 'PdmItem', 9200000000118, '00014', '充电桩后门', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '00014');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000119, 'PdmItem', 9200000000119, '00015', '充电枪底座', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '00015');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000120, 'PdmItem', 9200000000120, '00016', '充电枪挂耳', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '00016');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000121, 'PdmItem', 9200000000121, '00028', '铜包钢接地极', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '00028');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000122, 'PdmItem', 9200000000122, '00030', '发电柜', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '00030');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000123, 'PdmItem', 9200000000123, '12020200311', '机油壶', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12020200311');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000124, 'PdmItem', 9200000000124, '12020300269', '端头', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12020300269');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000125, 'PdmItem', 9200000000125, '14000100001', '底部滑轨', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '14000100001');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000126, 'PdmItem', 9200000000126, '14000100002', '镀锡', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '14000100002');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000127, 'PdmItem', 9200000000127, '14000100003', '后支撑弯角', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '14000100003');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000128, 'PdmItem', 9200000000128, '14000100004', '吊环', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '14000100004');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000129, 'PdmItem', 9200000000129, '14000100005', '电容叠母正', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '14000100005');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000130, 'PdmItem', 9200000000130, '14000100006', '电容叠母负', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '14000100006');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000131, 'PdmItem', 9200000000131, '14000100007', 'DSP芯片更换焊接', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '14000100007');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000132, 'PdmItem', 9200000000132, '14000100008', '配电柜', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '14000100008');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000133, 'PdmItem', 9200000000133, '14000100011', '充电桩logo丝网印', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '14000100011');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000134, 'PdmItem', 9200000000134, '14000300001', '标牌加工费', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '14000300001');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000135, 'PdmItem', 9200000000135, '14000300002', '铭牌', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '14000300002');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000136, 'PdmItem', 9200000000136, '14000300003', '铭牌', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '14000300003');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000137, 'PdmItem', 9200000000137, '14000300004', '标牌', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '14000300004');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000138, 'PdmItem', 9200000000138, '14000300005', 'S2024109铭牌标牌', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '14000300005');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000139, 'PdmItem', 9200000000139, '14000300006', '标牌', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '14000300006');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000000140, 'PdmItem', 9200000000140, 'Y9010000001', '测试', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = 'Y9010000001');

INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000002, 'CIS', 'LC1D245 3P', '', '16', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000002)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000002);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000003, 'CIS', 'E2N20 AC690V/2000A 50HZ 3P 固定式', '', '16', NULL, 'MAKE', '', '109976538421', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000003)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000003);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000004, 'CIS', 'E2N AC690V /2000A 3P 50HZ 固定式 水平出线', '', '16', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000004)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000004);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000005, 'CIS', 'MT20H2 3P  详见主要参数', '', '16', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000005)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000005);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000006, 'CIS', 'MT32H2 3P  详见主要参数', '', '16', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000006)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000006);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000007, 'CIS', 'GSW1-2000/3P  详见重要参数', '', '16', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000007)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000007);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000008, 'CIS', 'S3H250 R250+欠压AC220V脱扣器+直动式电操', '', '4', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000008)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000008);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000009, 'CIS', 'E1B1250R1250PR121/P-LSI3PFHRNST', '', '16', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000009)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000009);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000010, 'CIS', 'S5N400 R400 TM FF 3P +YU+电操 +2辅助触点', '', '4', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000010)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000010);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000011, 'CIS', 'S1N125 R125 TM 101th  FF 3P +YU+电操 +2辅助触点', '', '4', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000011)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000011);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000012, 'CIS', 'S3N250 R250 TM 101th  FF 3P +YU+电操 +2辅助触点', '', '4', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000012)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000012);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000013, 'CIS', 'S3N160 R160 TM 101th  FF 3P +YU+电操 +2辅助触点', '', '4', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000013)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000013);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000014, 'CIS', 'S2N160 R160 TM 101th  FFC 3P +YU+电操 +2辅助触点', '', '4', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000014)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000014);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000015, 'CIS', 'TM6.5/5H(1-10)', '', '30', NULL, 'MAKE', '', '', 'Using', 9000000000122
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000015)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000015);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000016, 'CIS', 'JGFM12-65', '', '18', NULL, 'MAKE', '', '', 'Using', 9000000000228
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000016)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000016);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000017, 'SCRAP', '红铜', '', '11', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000017)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000017);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000018, 'SCRAP', '', '', '11', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000018)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000018);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000019, 'SCRAP', '镀白', '', '11', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000019)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000019);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000020, 'TOOL', 'Φ8/T2Y', '', '38', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000020)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000020);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000021, 'TOOL', '0.8/Q235-A', '', '11', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000021)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000021);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000022, 'TOOL', '1.2/Q235-A', '', '11', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000022)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000022);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000023, 'TOOL', '1.0/ST12', '', '11', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000023)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000023);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000024, 'TOOL', '', '', '11', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000024)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000024);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000025, 'TOOL', '2.0/ST12', '', '11', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000025)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000025);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000026, 'TOOL', '2.0/Q235A', '', '11', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000026)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000026);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000027, 'TOOL', '2.5/Q235A', '', '11', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000027)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000027);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000028, 'TOOL', '3.0/Q235A', '', '11', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000028)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000028);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000029, 'TOOL', '', '', '11', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000029)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000029);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000030, 'TOOL', '2.0/1Cr18Ni9', '', '11', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000030)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000030);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000031, 'TOOL', '1.5/Q235A', '', '11', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000031)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000031);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000032, 'TOOL', '1.5/1Cr18Ni9', '', '11', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000032)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000032);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000033, 'TOOL', '0.5/1Cr18Ni9', '', '11', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000033)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000033);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000034, 'TOOL', '1.5/ST12', '', '11', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000034)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000034);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000035, 'SERVICE', '3S1003012', '', '4', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000035)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000035);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000036, 'SERVICE', '3S1003013', '', '4', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000036)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000036);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000037, 'SERVICE', '安装', '', '19', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000037)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000037);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000038, 'SERVICE', '包装运输', '', '19', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000038)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000038);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000039, 'SERVICE', '辅材人工', '', '19', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000039)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000039);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000040, 'SERVICE', '包装费', '', '16', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000040)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000040);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000041, 'SERVICE', 'DN25', '', '4', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000041)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000041);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000042, 'SERVICE', '', '', '38', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000042)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000042);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000043, 'SERVICE', '', '', '38', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000043)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000043);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000044, 'SERVICE', '', '', '19', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000044)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000044);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000045, 'SERVICE', '', '', '38', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000045)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000045);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000046, 'SERVICE', '', '', '44', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000046)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000046);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000047, 'SERVICE', '', '', '4', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000047)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000047);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000048, 'SERVICE', '', '', '44', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000048)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000048);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000049, 'SERVICE', '', '', '44', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000049)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000049);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000050, 'GAUGE', '9S184-117B', '', '6', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000050)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000050);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000051, 'GAUGE', '40*4', '', '38', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000051)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000051);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000052, 'GAUGE', 'YBLT-EKW/5A/B', '', '4', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000052)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000052);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000053, 'GAUGE', 'M8*40', '', '15', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000053)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000053);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000054, 'GAUGE', 'GB819-85 M8*60', '', '4', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000054)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000054);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000055, 'GAUGE', '8', '', '15', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000055)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000055);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000056, 'GAUGE', 'GB 955 30', '', '4', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000056)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000056);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000057, 'GAUGE', 'M8', '', '15', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000057)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000057);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000058, 'GAUGE', 'M8', '', '15', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000058)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000058);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000059, 'GAUGE', '', '', '6', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000059)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000059);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000060, 'GAUGE', '', '', '6', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000060)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000060);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000061, 'GAUGE', '', '', '6', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000061)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000061);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000062, 'GAUGE', '', '', '6', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000062)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000062);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000063, 'GAUGE', '', '', '6', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000063)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000063);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000064, 'GAUGE', '', '', '6', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000064)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000064);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000065, 'PART', '5CT.084.525.30', '', '6', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000065)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000065);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000066, 'PART', '0CT.325.503', '', '21', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000066)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000066);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000067, 'PART', '0CT.354.544.6', '', '21', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000067)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000067);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000068, 'PART', '0CT.354.544.7', '', '21', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000068)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000068);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000069, 'PART', '', '', '5', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000069)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000069);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000070, 'PART', '', '', '5', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000070)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000070);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000071, 'PART', '', '6063-T5', '6', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000071)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000071);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000072, 'PART', '0CT.354.002J', '', '6', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000072)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000072);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000073, 'PART', '0CT.310.001J', '', '6', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000073)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000073);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000074, 'PART', '0CT.310.002J', '', '6', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000074)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000074);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000075, 'PART', '0CT.310.730.1', '', '6', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000075)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000075);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000076, 'PART', '0CT.310.730.2', '', '6', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000076)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000076);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000077, 'PART', '0CT.310.731.1-3', '', '4', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000077)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000077);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000078, 'PART', '0CT.310.734.1-3', '', '4', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000078)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000078);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000079, 'PART', '0CT.310.737', '', '6', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000079)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000079);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000080, 'PRODUCT', '', '', '6', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000080)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000080);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000081, 'PRODUCT', '10S100-200Z', '', '6', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000081)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000081);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000082, 'PRODUCT', '', '', '6', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000082)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000082);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000083, 'PRODUCT', '', '', '6', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000083)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000083);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000084, 'PRODUCT', '', '', '6', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000084)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000084);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000085, 'PRODUCT', '17S080-001DT', '', '6', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000085)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000085);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000086, 'PRODUCT', '17S080-001J', '', '6', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000086)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000086);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000087, 'PRODUCT', '', '', '6', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000087)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000087);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000088, 'PRODUCT', '17S120-100Z', '', '6', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000088)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000088);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000089, 'PRODUCT', '17S120-200Z', 'TMY 5*50', '6', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000089)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000089);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000090, 'PRODUCT', '17S123-100TM', '', '6', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000090)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000090);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000091, 'PRODUCT', '17S125-100DT', '', '6', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000091)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000091);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000092, 'PRODUCT', '17S125-100J1', '', '6', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000092)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000092);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000093, 'PRODUCT', '17S125-100J2', '', '6', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000093)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000093);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000094, 'PRODUCT', '17S125-100J3', '', '6', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000094)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000094);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000095, 'MANUAL', '', '', '4', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000095)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000095);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000096, 'MANUAL', '', '', '4', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000096)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000096);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000097, 'MANUAL', '', '', '4', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000097)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000097);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000098, 'MANUAL', '', '', '4', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000098)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000098);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000099, 'MANUAL', '11', '', '4', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000099)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000099);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000100, 'MANUAL', '', '', '16', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000100)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000100);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000101, 'MANUAL', '', '', '4', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000101)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000101);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000102, 'MANUAL', '', '', '44', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000102)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000102);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000103, 'MANUAL', '', '', '4', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000103)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000103);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000104, 'MANUAL', '', '', '4', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000104)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000104);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000105, 'MANUAL', '', '', '4', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000105)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000105);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000106, 'MANUAL', '', '', '4', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000106)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000106);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000107, 'MANUAL', '', '', '4', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000107)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000107);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000108, 'MANUAL', '', '', '4', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000108)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000108);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000109, 'MANUAL', '', '', '4', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000109)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000109);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000110, 'STANDARD', '300mm*400mm', '', '15', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000110)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000110);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000111, 'STANDARD', '400mm*500mm', '', '15', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000111)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000111);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000112, 'STANDARD', '套筒过渡头 1/4', '', '15', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000112)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000112);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000113, 'STANDARD', '司钻房长*宽*高4900*2900*2900', '', '16', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000113)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000113);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000114, 'STANDARD', '15探头11云台', '', '19', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000114)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000114);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000115, 'STANDARD', '司钻房 钻台和二层台对讲系统', '', '19', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000115)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000115);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000116, 'STANDARD', '250A DC200/750', '', '22', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000116)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000116);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000117, 'STANDARD', '定制', '', '4', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000117)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000117);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000118, 'STANDARD', '定制', '', '4', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000118)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000118);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000119, 'STANDARD', '直流无芯/YG349-02-00-00KR', '', '4', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000119)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000119);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000120, 'STANDARD', '适配充电桩', '', '4', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000120)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000120);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000121, 'STANDARD', 'Φ20mm 1.2m', '', '25', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000121)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000121);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000122, 'STANDARD', '2200*500*800', '', '16', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000122)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000122);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000123, 'STANDARD', '机油壶', '', '22', NULL, 'MAKE', '', '', 'Using', 9000000000302
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000123)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000123);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000124, 'STANDARD', 'OTZ25-6', '', '4', NULL, 'MAKE', '', '', 'Using', 9000000000285
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000124)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000124);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000125, 'OUTSOURCE', '28S005-012L', '', '6', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000125)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000125);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000126, 'OUTSOURCE', '', '', '11', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000126)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000126);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000127, 'OUTSOURCE', '28S005-011L', '', '6', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000127)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000127);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000128, 'OUTSOURCE', '28S005-031L', '', '5', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000128)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000128);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000129, 'OUTSOURCE', '28S005-003L', '', '6', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000129)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000129);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000130, 'OUTSOURCE', '28S005-004L', '', '6', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000130)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000130);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000131, 'OUTSOURCE', 'TMS32028335PGFA', '', '4', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000131)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000131);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000132, 'OUTSOURCE', '650*720*250', '', '16', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000132)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000132);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000133, 'OUTSOURCE', '', '', '6', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000133)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000133);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000134, 'OUTSOURCE', '5S1002-050019L1-3', '', '19', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000134)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000134);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000135, 'OUTSOURCE', '123*78', '', '30', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000135)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000135);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000136, 'OUTSOURCE', '116*96', '', '30', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000136)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000136);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000137, 'OUTSOURCE', '48*14', '', '30', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000137)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000137);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000138, 'OUTSOURCE', '3S1058', '', '4', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000138)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000138);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000139, 'OUTSOURCE', '45*10', '', '4', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000139)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000139);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000000140, 'PART', '测试', '', '4', NULL, 'MAKE', '', '', 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000000140)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000000140);

COMMIT;

-- 蓝本中跨类型重复的编码（已追加序号避免撞唯一索引）：
--   15000100003
--   12040800007
