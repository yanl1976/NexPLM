-- =====================================================================
-- BOM 样例迁移：蓝本 PSEBOMLINE → bom_line
-- 生成器：tools/Gen-BomMigration.ps1（只读蓝本，仅生成 SQL，不执行）
-- 数据源：198.168.6.26,1433/TCPLM；生成时间：2026-11-08 15:24:20
-- 统计：取 100 条 → 生成 100 条；跳过 0 条
-- 口径：
--   1) 视图 DESIGN；BOM 类型 QUOTE（蓝本 LBOMTYPE 全为 Quote）
--   2) qty 一律 1：蓝本 USEQTY/TOTALQTY 实测全为 0/空，不编造数量
--   3) line_no 取 LSEQUENCE；ref_designator 取 POSNO；remark 取 LDESCRIPTION
--   4) 幂等：按 (parent, child, DESIGN) 判重
-- 注意：执行顺序 030 → 040 → 050（050 依赖前两份的 id 映射）
-- =====================================================================

BEGIN;

INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000001, 9200000001001, 9200000001002, 'DESIGN', 10, 1, '18', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001001 AND child_object_id = 9200000001002
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000002, 9200000001003, 9200000001004, 'DESIGN', 10, 1, '15', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001003 AND child_object_id = 9200000001004
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000003, 9200000001005, 9200000001006, 'DESIGN', 190, 1, '15', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001005 AND child_object_id = 9200000001006
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000004, 9200000001005, 9200000001007, 'DESIGN', 200, 1, '15', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001005 AND child_object_id = 9200000001007
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000005, 9200000001008, 9200000001009, 'DESIGN', 10, 1, '4', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001008 AND child_object_id = 9200000001009
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000006, 9200000001008, 9200000001010, 'DESIGN', 20, 1, '4', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001008 AND child_object_id = 9200000001010
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000007, 9200000001011, 9200000001012, 'DESIGN', 10, 1, '4', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001011 AND child_object_id = 9200000001012
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000008, 9200000001011, 9200000001013, 'DESIGN', 20, 1, '4', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001011 AND child_object_id = 9200000001013
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000009, 9200000001011, 9200000001014, 'DESIGN', 30, 1, '4', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001011 AND child_object_id = 9200000001014
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000010, 9200000001011, 9200000001015, 'DESIGN', 40, 1, '4', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001011 AND child_object_id = 9200000001015
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000011, 9200000001011, 9200000001016, 'DESIGN', 50, 1, '4', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001011 AND child_object_id = 9200000001016
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000012, 9200000001011, 9200000001017, 'DESIGN', 60, 1, '15', 0, 'Q3.Q4.Q24', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001011 AND child_object_id = 9200000001017
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000013, 9200000001011, 9200000001018, 'DESIGN', 70, 1, '15', 0, 'Q5.Q6', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001011 AND child_object_id = 9200000001018
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000014, 9200000001011, 9200000001019, 'DESIGN', 80, 1, '4', 0, 'F1-F20', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001011 AND child_object_id = 9200000001019
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000015, 9200000001011, 9200000001020, 'DESIGN', 90, 1, '4', 0, 'F1-F20', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001011 AND child_object_id = 9200000001020
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000016, 9200000001011, 9200000001021, 'DESIGN', 100, 1, '15', 0, 'TA1-TA6', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001011 AND child_object_id = 9200000001021
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000017, 9200000001011, 9200000001022, 'DESIGN', 110, 1, '4', 0, 'T2 T4', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001011 AND child_object_id = 9200000001022
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000018, 9200000001011, 9200000001023, 'DESIGN', 120, 1, '4', 0, 'T1 T3', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001011 AND child_object_id = 9200000001023
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000019, 9200000001011, 9200000001024, 'DESIGN', 130, 1, '4', 0, 'K1-K2', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001011 AND child_object_id = 9200000001024
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000020, 9200000001011, 9200000001025, 'DESIGN', 140, 1, '4', 0, 'K1-K2', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001011 AND child_object_id = 9200000001025
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000021, 9200000001011, 9200000001026, 'DESIGN', 150, 1, '4', 0, 'K1-K2', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001011 AND child_object_id = 9200000001026
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000022, 9200000001011, 9200000001027, 'DESIGN', 160, 1, '4', 0, 'K1-K2', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001011 AND child_object_id = 9200000001027
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000023, 9200000001011, 9200000001028, 'DESIGN', 170, 1, '4', 0, 'K1-K2', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001011 AND child_object_id = 9200000001028
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000024, 9200000001011, 9200000001029, 'DESIGN', 180, 1, '4', 0, 'P1-P2', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001011 AND child_object_id = 9200000001029
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000025, 9200000001011, 9200000001030, 'DESIGN', 190, 1, '15', 0, 'H1.H3', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001011 AND child_object_id = 9200000001030
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000026, 9200000001011, 9200000001031, 'DESIGN', 200, 1, '15', 0, 'H2.H4 H5-H21', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001011 AND child_object_id = 9200000001031
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000027, 9200000001011, 9200000001032, 'DESIGN', 210, 1, '4', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001011 AND child_object_id = 9200000001032
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000028, 9200000001011, 9200000001033, 'DESIGN', 220, 1, '4', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001011 AND child_object_id = 9200000001033
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000029, 9200000001011, 9200000001034, 'DESIGN', 230, 1, '4', 0, 'X1.X2.X3', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001011 AND child_object_id = 9200000001034
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000030, 9200000001011, 9200000001035, 'DESIGN', 240, 1, '4', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001011 AND child_object_id = 9200000001035
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000031, 9200000001011, 9200000001036, 'DESIGN', 250, 1, '30', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001011 AND child_object_id = 9200000001036
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000032, 9200000001011, 9200000001037, 'DESIGN', 260, 1, '30', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001011 AND child_object_id = 9200000001037
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000033, 9200000001011, 9200000001038, 'DESIGN', 270, 1, '4', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001011 AND child_object_id = 9200000001038
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000034, 9200000001011, 9200000001039, 'DESIGN', 280, 1, '4', 0, 'Q17-Q18', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001011 AND child_object_id = 9200000001039
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000035, 9200000001011, 9200000001040, 'DESIGN', 290, 1, '4', 0, 'Q1-Q2', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001011 AND child_object_id = 9200000001040
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000036, 9200000001011, 9200000001041, 'DESIGN', 300, 1, '15', 0, 'ATS', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001011 AND child_object_id = 9200000001041
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000037, 9200000001011, 9200000001042, 'DESIGN', 310, 1, '4', 0, 'Q13-Q14', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001011 AND child_object_id = 9200000001042
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000038, 9200000001011, 9200000001043, 'DESIGN', 320, 1, '4', 0, 'Q19-Q23', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001011 AND child_object_id = 9200000001043
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000039, 9200000001011, 9200000001044, 'DESIGN', 330, 1, '4', 0, '交设计', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001011 AND child_object_id = 9200000001044
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000040, 9200000001011, 9200000001045, 'DESIGN', 340, 1, '4', 0, 'Q15-Q16', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001011 AND child_object_id = 9200000001045
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000041, 9200000001011, 9200000001046, 'DESIGN', 350, 1, '4', 0, 'Q7-Q12', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001011 AND child_object_id = 9200000001046
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000042, 9200000001047, 9200000001048, 'DESIGN', 10, 1, '38', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001047 AND child_object_id = 9200000001048
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000043, 9200000001047, 9200000001049, 'DESIGN', 20, 1, '38', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001047 AND child_object_id = 9200000001049
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000044, 9200000001047, 9200000001050, 'DESIGN', 30, 1, '38', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001047 AND child_object_id = 9200000001050
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000045, 9200000001047, 9200000001051, 'DESIGN', 40, 1, '38', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001047 AND child_object_id = 9200000001051
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000046, 9200000001047, 9200000001052, 'DESIGN', 50, 1, '38', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001047 AND child_object_id = 9200000001052
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000047, 9200000001047, 9200000001053, 'DESIGN', 60, 1, '38', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001047 AND child_object_id = 9200000001053
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000048, 9200000001047, 9200000001054, 'DESIGN', 70, 1, '38', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001047 AND child_object_id = 9200000001054
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000049, 9200000001047, 9200000001055, 'DESIGN', 80, 1, '38', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001047 AND child_object_id = 9200000001055
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000050, 9200000001047, 9200000001056, 'DESIGN', 90, 1, '38', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001047 AND child_object_id = 9200000001056
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000051, 9200000001057, 9200000001058, 'DESIGN', 10, 1, '4', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001057 AND child_object_id = 9200000001058
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000052, 9200000001057, 9200000001059, 'DESIGN', 20, 1, '4', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001057 AND child_object_id = 9200000001059
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000053, 9200000001057, 9200000001060, 'DESIGN', 30, 1, '4', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001057 AND child_object_id = 9200000001060
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000054, 9200000001057, 9200000001061, 'DESIGN', 40, 1, '6', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001057 AND child_object_id = 9200000001061
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000055, 9200000001057, 9200000001062, 'DESIGN', 50, 1, '34', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001057 AND child_object_id = 9200000001062
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000056, 9200000001057, 9200000001063, 'DESIGN', 60, 1, '6', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001057 AND child_object_id = 9200000001063
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000057, 9200000001057, 9200000001064, 'DESIGN', 70, 1, '4', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001057 AND child_object_id = 9200000001064
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000058, 9200000001057, 9200000001065, 'DESIGN', 80, 1, '4', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001057 AND child_object_id = 9200000001065
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000059, 9200000001057, 9200000001066, 'DESIGN', 90, 1, '4', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001057 AND child_object_id = 9200000001066
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000060, 9200000001057, 9200000001067, 'DESIGN', 100, 1, '4', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001057 AND child_object_id = 9200000001067
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000061, 9200000001057, 9200000001068, 'DESIGN', 110, 1, '4', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001057 AND child_object_id = 9200000001068
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000062, 9200000001069, 9200000001070, 'DESIGN', 10, 1, '19', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001069 AND child_object_id = 9200000001070
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000063, 9200000001069, 9200000001071, 'DESIGN', 20, 1, '6', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001069 AND child_object_id = 9200000001071
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000064, 9200000001069, 9200000001072, 'DESIGN', 30, 1, '4', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001069 AND child_object_id = 9200000001072
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000065, 9200000001069, 9200000001073, 'DESIGN', 40, 1, '6', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001069 AND child_object_id = 9200000001073
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000066, 9200000001069, 9200000001074, 'DESIGN', 50, 1, '19', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001069 AND child_object_id = 9200000001074
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000067, 9200000001069, 9200000001075, 'DESIGN', 60, 1, '4', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001069 AND child_object_id = 9200000001075
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000068, 9200000001069, 9200000001076, 'DESIGN', 70, 1, '19', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001069 AND child_object_id = 9200000001076
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000069, 9200000001069, 9200000001077, 'DESIGN', 80, 1, '19', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001069 AND child_object_id = 9200000001077
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000070, 9200000001069, 9200000001078, 'DESIGN', 90, 1, '6', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001069 AND child_object_id = 9200000001078
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000071, 9200000001069, 9200000001079, 'DESIGN', 100, 1, '6', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001069 AND child_object_id = 9200000001079
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000072, 9200000001069, 9200000001080, 'DESIGN', 110, 1, '6', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001069 AND child_object_id = 9200000001080
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000073, 9200000001081, 9200000001082, 'DESIGN', 10, 1, '4', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001081 AND child_object_id = 9200000001082
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000074, 9200000001083, 9200000001084, 'DESIGN', 10, 1, '4', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001083 AND child_object_id = 9200000001084
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000075, 9200000001085, 9200000001086, 'DESIGN', 10, 1, '38', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001085 AND child_object_id = 9200000001086
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000076, 9200000001087, 9200000001088, 'DESIGN', 10, 1, '4', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001087 AND child_object_id = 9200000001088
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000077, 9200000001089, 9200000001090, 'DESIGN', 10, 1, '4', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001089 AND child_object_id = 9200000001090
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000078, 9200000000138, 9200000000136, 'DESIGN', 10, 1, '30', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000000138 AND child_object_id = 9200000000136
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000079, 9200000000138, 9200000000139, 'DESIGN', 20, 1, '4', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000000138 AND child_object_id = 9200000000139
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000080, 9200000001091, 9200000001092, 'DESIGN', 10, 1, '18', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001091 AND child_object_id = 9200000001092
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000081, 9200000001093, 9200000001094, 'DESIGN', 10, 1, '16', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001093 AND child_object_id = 9200000001094
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000082, 9200000001093, 9200000001095, 'DESIGN', 20, 1, '4', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001093 AND child_object_id = 9200000001095
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000083, 9200000001093, 9200000001096, 'DESIGN', 30, 1, '21', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001093 AND child_object_id = 9200000001096
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000084, 9200000001093, 9200000001097, 'DESIGN', 40, 1, '18', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001093 AND child_object_id = 9200000001097
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000085, 9200000001093, 9200000001098, 'DESIGN', 50, 1, '18', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001093 AND child_object_id = 9200000001098
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000086, 9200000001093, 9200000001099, 'DESIGN', 60, 1, '18', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001093 AND child_object_id = 9200000001099
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000087, 9200000001093, 9200000001100, 'DESIGN', 70, 1, '18', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001093 AND child_object_id = 9200000001100
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000088, 9200000001093, 9200000001101, 'DESIGN', 80, 1, '16', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001093 AND child_object_id = 9200000001101
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000089, 9200000001093, 9200000001102, 'DESIGN', 90, 1, '4', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001093 AND child_object_id = 9200000001102
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000090, 9200000001093, 9200000001103, 'DESIGN', 100, 1, '4', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001093 AND child_object_id = 9200000001103
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000091, 9200000001093, 9200000001104, 'DESIGN', 110, 1, '16', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001093 AND child_object_id = 9200000001104
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000092, 9200000001093, 9200000001105, 'DESIGN', 120, 1, '18', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001093 AND child_object_id = 9200000001105
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000093, 9200000001093, 9200000001106, 'DESIGN', 130, 1, '18', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001093 AND child_object_id = 9200000001106
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000094, 9200000001093, 9200000001107, 'DESIGN', 140, 1, '18', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001093 AND child_object_id = 9200000001107
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000095, 9200000001093, 9200000001108, 'DESIGN', 150, 1, '15', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001093 AND child_object_id = 9200000001108
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000096, 9200000001093, 9200000001109, 'DESIGN', 160, 1, '4', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001093 AND child_object_id = 9200000001109
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000097, 9200000001093, 9200000001110, 'DESIGN', 170, 1, '15', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001093 AND child_object_id = 9200000001110
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000098, 9200000001093, 9200000001111, 'DESIGN', 180, 1, '4', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001093 AND child_object_id = 9200000001111
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000099, 9200000001093, 9200000001112, 'DESIGN', 190, 1, '4', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001093 AND child_object_id = 9200000001112
                    AND view_type = 'DESIGN' AND deleted = false);
INSERT INTO bom_line (id, parent_object_id, child_object_id, view_type, line_no, qty, unit,
                        loss_rate, ref_designator, remark, version, deleted, effect_from, effect_to,
                        total_qty, surplus_qty, sub_use_qty, bom_type, item_route, data_source,
                        created_by, created_at, updated_by, updated_at)
SELECT 9300000000100, 9200000001093, 9200000001113, 'DESIGN', 200, 1, '4', 0, '', '', 0, false, NULL, NULL,
       NULL, NULL, NULL, 'QUOTE', NULL, 'IMPORT', 1, now(), 1, now()
 WHERE NOT EXISTS (SELECT 1 FROM bom_line WHERE parent_object_id = 9200000001093 AND child_object_id = 9200000001113
                    AND view_type = 'DESIGN' AND deleted = false);

COMMIT;
