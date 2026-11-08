-- =====================================================================
-- BOM 迁移补件：050 引用的父子件中，未包含在 030 样例里的零件
-- 生成器：tools/Gen-BomMigration.ps1（只读蓝本，仅生成 SQL，不执行）
-- 数据源：198.168.6.26,1433/TCPLM；生成时间：2026-11-08 15:24:20
-- 补迁零件 113 个（父件走 PSEITEM、子件走 PSEITEMMASTER 视图）
-- 映射与 030 一致：ITEMTYPE → itemType、GGXH → specification、ORIGIN=2 → BUY
-- =====================================================================

BEGIN;

INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001001, 'PdmItem', 9200000001001, '10010100273', '7寸屏幕', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10010100273');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001002, 'PdmItem', 9200000001002, '12040501313', '7寸屏幕', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12040501313');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001003, 'PdmItem', 9200000001003, '10020100981', '交流接触器', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10020100981');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001004, 'PdmItem', 9200000001004, '10020100684', '接触器', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10020100684');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001005, 'PdmItem', 9200000001005, '10040100045', '熔座', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10040100045');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001006, 'PdmItem', 9200000001006, '10040100096', '熔座', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10040100096');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001007, 'PdmItem', 9200000001007, '10050101076', '电流互感器', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10050101076');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001008, 'PdmItem', 9200000001008, '10180802747', '编码器', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10180802747');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001009, 'PdmItem', 9200000001009, '10180802746', '编码器', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10180802746');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001010, 'PdmItem', 9200000001010, '10180802745', '编码器', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10180802745');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001011, 'PdmItem', 9200000001011, '11010100033', 'CBC1总装', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '11010100033');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001012, 'PdmItem', 9200000001012, '12020100037', '户外配电柜', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12020100037');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001013, 'PdmItem', 9200000001013, '12020500316', '不锈钢格兰', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12020500316');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001014, 'PdmItem', 9200000001014, '12020500315', '不锈钢格兰', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12020500315');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001015, 'PdmItem', 9200000001015, '12020500308', '不锈钢格兰', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12020500308');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001016, 'PdmItem', 9200000001016, '12020500274', '不锈钢格兰', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12020500274');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001017, 'PdmItem', 9200000001017, '10010500267', '微型断路器', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10010500267');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001018, 'PdmItem', 9200000001018, '10010500692', '微型断路器', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10010500692');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001019, 'PdmItem', 9200000001019, '10040200073', '熔座', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10040200073');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001020, 'PdmItem', 9200000001020, '10040300319', '熔芯', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10040300319');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001021, 'PdmItem', 9200000001021, '10050100306', '电流互感器', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10050100306');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001022, 'PdmItem', 9200000001022, '10070100337', '三相变压器', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10070100337');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001023, 'PdmItem', 9200000001023, '10070200248', '单相变压器', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10070200248');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001024, 'PdmItem', 9200000001024, '10080200108', '继电器', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10080200108');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001025, 'PdmItem', 9200000001025, '10080200110', '防浪涌保护插件', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10080200110');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001026, 'PdmItem', 9200000001026, '10080200111', '金属保护卡子', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10080200111');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001027, 'PdmItem', 9200000001027, '10080200270', '标签位', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10080200270');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001028, 'PdmItem', 9200000001028, '10080400034', '继电器基座', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10080400034');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001029, 'PdmItem', 9200000001029, '10091000130', '三相多功能仪表', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10091000130');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001030, 'PdmItem', 9200000001030, '10110100091', '指示灯', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10110100091');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001031, 'PdmItem', 9200000001031, '10110100180', '指示灯', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10110100180');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001032, 'PdmItem', 9200000001032, '10160100140', '标记端子', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10160100140');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001033, 'PdmItem', 9200000001033, '10160100412', '端子固定器', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10160100412');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001034, 'PdmItem', 9200000001034, '10160100929', '弹簧压接端子', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10160100929');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001035, 'PdmItem', 9200000001035, '10160100931', '隔板', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10160100931');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001036, 'PdmItem', 9200000001036, '10160101390', '标记号', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10160101390');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001037, 'PdmItem', 9200000001037, '10160101391', '标记号', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10160101391');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001038, 'PdmItem', 9200000001038, '10160300145', '插拔式桥接件', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10160300145');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001039, 'PdmItem', 9200000001039, '10010600259', '漏电断路器', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10010600259');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001040, 'PdmItem', 9200000001040, '10010100261', '框架断路器', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10010100261');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001041, 'PdmItem', 9200000001041, '10200400213', 'ATS控制器', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10200400213');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001042, 'PdmItem', 9200000001042, '10010600001', '漏电断路器', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10010600001');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001043, 'PdmItem', 9200000001043, '10010600260', '漏电断路器', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10010600260');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001044, 'PdmItem', 9200000001044, '10180800883', 'U盘', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10180800883');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001045, 'PdmItem', 9200000001045, '10010600258', '漏电断路器', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10010600258');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001046, 'PdmItem', 9200000001046, '10010600002', '漏电断路器', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10010600002');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001047, 'PdmItem', 9200000001047, '11030202098', '库存电缆调拨', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '11030202098');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001048, 'PdmItem', 9200000001048, '11030201809', '电缆', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '11030201809');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001049, 'PdmItem', 9200000001049, '110300011', '仪表电缆(蓝色塑料外皮)', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '110300011');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001050, 'PdmItem', 9200000001050, '11030201842', '低温电缆', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '11030201842');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001051, 'PdmItem', 9200000001051, '11030201629', '耐寒电缆', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '11030201629');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001052, 'PdmItem', 9200000001052, '11030201519', '电缆', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '11030201519');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001053, 'PdmItem', 9200000001053, '11030201628', '耐寒电缆', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '11030201628');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001054, 'PdmItem', 9200000001054, '11030201636', '耐寒电缆', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '11030201636');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001055, 'PdmItem', 9200000001055, '11030201638', '耐寒电缆', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '11030201638');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001056, 'PdmItem', 9200000001056, '11030201522', '电缆', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '11030201522');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001057, 'PdmItem', 9200000001057, '12010201001', '十字槽锌白锌沉头', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12010201001');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001058, 'PdmItem', 9200000001058, '12020300428', '端头', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12020300428');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001059, 'PdmItem', 9200000001059, '12020300347', '端头', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12020300347');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001060, 'PdmItem', 9200000001060, '12020300160', '端头', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12020300160');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001061, 'PdmItem', 9200000001061, '12020300004', '端头', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12020300004');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001062, 'PdmItem', 9200000001062, '12020300103', '端头', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12020300103');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001063, 'PdmItem', 9200000001063, '12020300073', '端头', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12020300073');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001064, 'PdmItem', 9200000001064, '12020300074', '端头', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12020300074');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001065, 'PdmItem', 9200000001065, '12010600037', '弹垫', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12010600037');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001066, 'PdmItem', 9200000001066, '12010500125', '大外径平垫', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12010500125');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001067, 'PdmItem', 9200000001067, '11040300151', '绝缘子（高强）', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '11040300151');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001068, 'PdmItem', 9200000001068, '12010200992', '螺钉', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12010200992');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001069, 'PdmItem', 9200000001069, '12010201523', '母线桥螺钉', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12010201523');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001070, 'PdmItem', 9200000001070, '12010400211', '高强度成套六角螺栓', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12010400211');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001071, 'PdmItem', 9200000001071, '12010400206', '螺母', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12010400206');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001072, 'PdmItem', 9200000001072, '12010500169', '平垫', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12010500169');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001073, 'PdmItem', 9200000001073, '12010600021', '弹垫', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12010600021');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001074, 'PdmItem', 9200000001074, '12010300143', '高强度成套六角螺栓', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12010300143');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001075, 'PdmItem', 9200000001075, '12010400209', '高强度成套六角螺栓', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12010400209');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001076, 'PdmItem', 9200000001076, '12010201507', '高强度成套六角螺栓', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12010201507');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001077, 'PdmItem', 9200000001077, '12010201503', '高强度成套六角螺栓', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12010201503');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001078, 'PdmItem', 9200000001078, '12010400208', '螺母', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12010400208');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001079, 'PdmItem', 9200000001079, '12010500167', '平垫', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12010500167');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001080, 'PdmItem', 9200000001080, '12010600093', '弹垫', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12010600093');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001081, 'PdmItem', 9200000001081, '12020200775', '机用丝锥', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12020200775');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001082, 'PdmItem', 9200000001082, '12020200778', '机用丝锥', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12020200778');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001083, 'PdmItem', 9200000001083, '12040502409', '绝缘子 横梁', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12040502409');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001084, 'PdmItem', 9200000001084, '12040502413', '高压绝缘子', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12040502413');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001085, 'PdmItem', 9200000001085, '12040502853', '光纤', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12040502853');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001086, 'PdmItem', 9200000001086, '11030300253', '室外用光缆', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '11030300253');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001087, 'PdmItem', 9200000001087, '12040700198', '逃生灯', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12040700198');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001088, 'PdmItem', 9200000001088, '12040700197', '逃生灯', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12040700197');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001089, 'PdmItem', 9200000001089, '12040700233', '摄像机支架补单', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12040700233');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001090, 'PdmItem', 9200000001090, '12040700162', '球机支架', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12040700162');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001091, 'PdmItem', 9200000001091, '14000400001', 'A1028焊装外协', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '14000400001');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001092, 'PdmItem', 9200000001092, '12040400350', '转换板', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12040400350');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001093, 'PdmItem', 9200000001093, '20001Z', '单系统PLC柜', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '20001Z');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001094, 'PdmItem', 9200000001094, '12020100380', 'PLC柜', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12020100380');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001095, 'PdmItem', 9200000001095, '10200100829', 'CPU', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10200100829');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001096, 'PdmItem', 9200000001096, '10200100832', '存储卡', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10200100832');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001097, 'PdmItem', 9200000001097, '10200100733', '模拟量输入模块', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10200100733');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001098, 'PdmItem', 9200000001098, '10200100734', '模拟量输出模块', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10200100734');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001099, 'PdmItem', 9200000001099, '10200100731', '数字量输入模块', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10200100731');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001100, 'PdmItem', 9200000001100, '10200100732', '数字量输出模块', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10200100732');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001101, 'PdmItem', 9200000001101, '10200101082', '通讯模块', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10200101082');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001102, 'PdmItem', 9200000001102, '10200500094', '触摸屏', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10200500094');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001103, 'PdmItem', 9200000001103, '12040502132', '5口交换机', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '12040502132');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001104, 'PdmItem', 9200000001104, '10200100097', '接口模块', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10200100097');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001105, 'PdmItem', 9200000001105, '10200100737', '模块底座', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10200100737');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001106, 'PdmItem', 9200000001106, '10200100850', 'BU端子', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10200100850');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001107, 'PdmItem', 9200000001107, '10200100738', '模块底座', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10200100738');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001108, 'PdmItem', 9200000001108, '10151000102', '电源', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10151000102');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001109, 'PdmItem', 9200000001109, '10070400086', '控制变压器', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10070400086');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001110, 'PdmItem', 9200000001110, '10080200151', '中间继电器', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10080200151');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001111, 'PdmItem', 9200000001111, '10080400050', '继电器座', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10080400050');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001112, 'PdmItem', 9200000001112, '10180500189', '继电器金属保护卡子', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10180500189');
INSERT INTO plm_object (id, object_type, master_id, code, name, company_id, owner_id,
                          lifecycle_state, phase_state, workflow_state, revision, iteration,
                          is_latest, deleted, version, ext_json, created_by, created_at,
                          updated_by, updated_at)
SELECT 9200000001113, 'PdmItem', 9200000001113, '10080200441', '继电器', 1, 1, 'Draft', NULL, 'New', 1, 1,
       true, false, 0, '{}'::jsonb, 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM plm_object WHERE object_type = 'PdmItem' AND code = '10080200441');

INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001001, 'CIS', '', NULL, '18', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001001)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001001);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001002, 'STANDARD', 'DMT80480T070-16WT', NULL, '18', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001002)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001002);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001003, 'CIS', '', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001003)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001003);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001004, 'CIS', 'NXC-160/AC220V 50/60Hz', NULL, '15', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001004)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001004);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001005, 'CIS', 'RL98B-32/25A AC690V', NULL, '15', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001005)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001005);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001006, 'CIS', '20A/AC690V', NULL, '15', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001006)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001006);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001007, 'CIS', 'CHL1-0.66-80II 2000/5 0.2级', NULL, '15', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001007)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001007);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001008, 'CIS', '', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001008)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001008);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001009, 'CIS', 'M6C-4S8HX51ZTZ00', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001009)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001009);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001010, 'CIS', 'OCE-EIC1B-1213-F10S-HFZ-369', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001010)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001010);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001011, 'TOOL', '5S1014-61000022222', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001011)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001011);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001012, 'STANDARD', '1300*1000*2200mm（长宽高）', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001012)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001012);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001013, 'STANDARD', 'BDM-VIII-G2', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001013)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001013);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001014, 'STANDARD', 'BDM-VIII-G1 1/2', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001014)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001014);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001015, 'STANDARD', 'BDM-VIII-G1 1/4', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001015)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001015);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001016, 'STANDARD', 'BDM-VIII-G1', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001016)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001016);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001017, 'CIS', 'iC65N 2P C10A', NULL, '15', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001017)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001017);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001018, 'CIS', 'iC65N 4P C10A', NULL, '15', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001018)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001018);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001019, 'CIS', 'RS9-DRT10P', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001019)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001019);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001020, 'CIS', 'RS9A-A1110P-690V/2A', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001020)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001020);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001021, 'CIS', 'BH-0.66-60Ⅱ 800A/5A 0.2级', NULL, '15', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001021)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001021);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001022, 'CIS', 'Y/Y-12 50VA 400/100V  三相50Hz±5%空载', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001022)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001022);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001023, 'CIS', '400/220V  100VA  50Hz±5%空载', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001023)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001023);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001024, 'CIS', 'RXM3AB2P7', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001024)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001024);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001025, 'CIS', 'RXM041FU7', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001025)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001025);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001026, 'CIS', 'RXZ400', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001026)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001026);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001027, 'CIS', 'RXZL420', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001027)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001027);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001028, 'CIS', 'RXZE2S111M', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001028)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001028);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001029, 'CIS', 'SK284E-3S4  CT800/5  PT400/100  辅助电源AC220V', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001029)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001029);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001030, 'CIS', 'AD11-22/21-7GZ AC220V 白', NULL, '15', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001030)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001030);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001031, 'CIS', 'AD11-22/21-7GZ AC220V 红', NULL, '15', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001031)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001031);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001032, 'CIS', 'VTM10', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001032)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001032);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001033, 'CIS', 'VF4/5', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001033)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001033);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001034, 'CIS', 'RST2.5FO', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001034)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001034);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001035, 'CIS', 'RSP 2.5FO', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001035)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001035);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001036, 'CIS', 'TM10.5/5H（1-10）', NULL, '30', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001036)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001036);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001037, 'CIS', 'TM10.5/5H（11-20）', NULL, '30', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001037)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001037);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001038, 'CIS', 'RPL2-2.5', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001038)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001038);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001039, 'CIS', 'VIGI NSX 160F TMD 160 3P F EL 30mA', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001039)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001039);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001040, 'CIS', '3WT1S08 ETU35WT F/3P', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001040)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001040);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001041, 'CIS', 'ATS022', NULL, '15', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001041)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001041);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001042, 'CIS', 'iC65N C63A 3P +Vigi iC65 ELE 30mA', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001042)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001042);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001043, 'CIS', 'VIGI NSX 250F TMD 200 3P F EL 30mA', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001043)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001043);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001044, 'CIS', '8G', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001044)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001044);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001045, 'CIS', 'VIGI NSX 100F TMD 100 3P F EL 30mA', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001045)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001045);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001046, 'CIS', 'iC65N C25A 3P +Vigi iC65 ELE 30mA', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001046)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001046);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001047, 'TOOL', '', NULL, '38', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001047)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001047);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001048, 'TOOL', 'YCW 450/750V 3*25+1*10', NULL, '38', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001048)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001048);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001049, 'TOOL', 'WDZ-KEEPR-450/750V 3*0.5+P', NULL, '38', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001049)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001049);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001050, 'TOOL', 'YGU/D-50℃ 450V/750V 3*4', NULL, '38', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001050)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001050);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001051, 'TOOL', 'IEEE45/LSZH-60℃-450V/750V 3*4+1*2.5', NULL, '38', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001051)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001051);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001052, 'TOOL', 'YCW-450/750-10*1.0', NULL, '38', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001052)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001052);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001053, 'TOOL', 'IEEE45/LSZH-60℃-450V/750V 3*10+1*6', NULL, '38', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001053)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001053);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001054, 'TOOL', 'IEEE45/LSZH-60℃-450V/750V 3*35+1*25', NULL, '38', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001054)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001054);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001055, 'TOOL', 'IEEE45/LSZH-60℃-450V/750V 3*50+1*35', NULL, '38', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001055)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001055);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001056, 'TOOL', 'YCWP 450V/750V 20*1+P', NULL, '38', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001056)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001056);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001057, 'STANDARD', 'M5*16', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001057)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001057);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001058, 'STANDARD', 'UT 6-4  镀锡', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001058)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001058);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001059, 'STANDARD', 'VE  2.5mm^2   蓝色', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001059)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001059);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001060, 'STANDARD', 'OT 1.5-3  镀锡', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001060)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001060);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001061, 'STANDARD', 'OT 1.5-5', NULL, '6', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001061)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001061);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001062, 'STANDARD', 'OT 1.5-8', NULL, '34', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001062)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001062);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001063, 'STANDARD', 'OT 4-6', NULL, '6', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001063)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001063);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001064, 'STANDARD', 'OT 4-8', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001064)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001064);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001065, 'STANDARD', 'Φ4  彩锌', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001065)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001065);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001066, 'STANDARD', 'Φ5（镀白锌）', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001066)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001066);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001067, 'TOOL', 'MNS 50*50*M8', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001067)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001067);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001068, 'STANDARD', 'M5*16 圆头十字  彩锌', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001068)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001068);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001069, 'STANDARD', '3S1045', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001069)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001069);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001070, 'STANDARD', '镀锌10*35', NULL, '19', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001070)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001070);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001071, 'STANDARD', '镀锌M10', NULL, '6', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001071)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001071);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001072, 'STANDARD', '镀锌的Φ10', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001072)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001072);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001073, 'STANDARD', '镀锌的Φ10', NULL, '6', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001073)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001073);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001074, 'STANDARD', '镀锌10*50', NULL, '19', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001074)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001074);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001075, 'STANDARD', '镀锌16*70', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001075)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001075);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001076, 'STANDARD', '镀锌16*90', NULL, '19', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001076)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001076);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001077, 'STANDARD', '镀锌16*110', NULL, '19', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001077)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001077);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001078, 'STANDARD', '镀锌M16', NULL, '6', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001078)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001078);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001079, 'STANDARD', '镀锌Φ16', NULL, '6', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001079)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001079);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001080, 'STANDARD', '镀锌Φ16', NULL, '6', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001080)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001080);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001081, 'STANDARD', 'M4（机用丝锥）', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001081)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001081);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001082, 'STANDARD', 'M8', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001082)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001082);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001083, 'STANDARD', '', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001083)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001083);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001084, 'STANDARD', 'ZJ-12 95*145', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001084)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001084);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001085, 'STANDARD', '', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001085)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001085);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001086, 'TOOL', 'GYXTW-6A1b', NULL, '38', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001086)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001086);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001087, 'STANDARD', '', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001087)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001087);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001088, 'STANDARD', '220U  不锈钢', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001088)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001088);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001089, 'STANDARD', '', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001089)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001089);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001090, 'STANDARD', 'DS-1602ZJ', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001090)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001090);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001091, 'OUTSOURCE', '9S219-1028MX', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001091)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001091);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001092, 'STANDARD', 'A1027', NULL, '18', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001092)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001092);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001093, 'GAUGE', '001Z', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001093)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001093);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001094, 'STANDARD', '2200*800*600 高*宽*深', NULL, '16', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001094)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001094);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001095, 'CIS', 'CPU1511-1PN   6ES75111AK020AB0', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001095)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001095);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001096, 'CIS', '4MB 6ES79548LC030AA0', NULL, '21', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001096)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001096);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001097, 'CIS', '6ES7134-6GF00-0AA1', NULL, '18', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001097)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001097);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001098, 'CIS', '6ES7135-6HD00-0BA1', NULL, '18', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001098)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001098);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001099, 'CIS', '6ES7131-6BH01-0BA0', NULL, '18', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001099)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001099);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001100, 'CIS', '6ES7132-6BH01-0BA0', NULL, '18', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001100)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001100);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001101, 'CIS', '6ES7137-6AA01-0BA0', NULL, '16', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001101)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001101);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001102, 'CIS', 'TPC1031Kt 10 寸', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001102)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001102);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001103, 'STANDARD', '6GK5005-0BA00-1AB2', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001103)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001103);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001104, 'CIS', '6ES7155-6AA01-0BN0', NULL, '16', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001104)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001104);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001105, 'CIS', '6ES7193-6BP00-0DA0', NULL, '18', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001105)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001105);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001106, 'CIS', '6ES7193-6BP20-0DA0', NULL, '18', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001106)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001106);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001107, 'CIS', '6ES7193-6BP20-0BA0', NULL, '18', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001107)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001107);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001108, 'CIS', 'NDR-240-24', NULL, '15', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001108)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001108);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001109, 'CIS', 'NDK-100VA 220/24', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001109)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001109);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001110, 'CIS', 'RXM2LB2P7', NULL, '15', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001110)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001110);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001111, 'CIS', 'RXZE1M2C', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001111)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001111);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001112, 'CIS', 'RXZ410', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001112)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001112);
INSERT INTO item_master (object_id, item_type, specification, material, unit, weight,
                         source_type, std_code, remark, using_state, class_id)
SELECT 9200000001113, 'CIS', 'RXM2LB2BD', NULL, '4', NULL, 'MAKE', NULL, NULL, 'Using', NULL
 WHERE EXISTS (SELECT 1 FROM plm_object WHERE id = 9200000001113)
   AND NOT EXISTS (SELECT 1 FROM item_master WHERE object_id = 9200000001113);

COMMIT;
