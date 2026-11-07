-- =====================================================================
-- 批次 G·方案 A：ERP 客制字段落 PdmItem 元数据（值存 plm_object.ext_json）
--
-- 背景（docs/06 EXP-20261107-19 / 方案 A 决策）：
--   物料段 32 列里有 14 列、BOM 段 9 列里有 4 列在 item_master 中没有数据源。
--   方案 A 不动物理表，改为在 meta_field 里为 PdmItem 定义客制字段、
--   值写 plm_object.ext_json，ERP 映射的 sourcePath 写 ext_json.<字段码>。
--
-- 字段编码直接沿用金蝶 ERP 列名：这是与 ERP 的契约名，换成驼峰反而两边对不上。
-- =====================================================================

INSERT INTO meta_field (id, entity_code, code, name, value_type, data_type, required, enum_code,
                        ui_json, searchable, sort, version_no, status)
VALUES
    (903101, 'PdmItem', 'FErpClsID',        '物料属性', 'DefaultObjectValue', 'VARCHAR', false, NULL,
     '{}'::jsonb, true,  210, 1, 'PUBLISHED'),
    (903102, 'PdmItem', 'FCategoryID',      '存货类别', 'DefaultObjectValue', 'VARCHAR', false, NULL,
     '{}'::jsonb, true,  220, 1, 'PUBLISHED'),
    (903103, 'PdmItem', 'FPlanningStrategy', '计划策略', 'DefaultObjectValue', 'VARCHAR', false, NULL,
     '{}'::jsonb, false, 230, 1, 'PUBLISHED'),
    (903104, 'PdmItem', 'FIsProduce',       '允许生产', 'BoolValue',          'BOOLEAN', false, NULL,
     '{}'::jsonb, false, 240, 1, 'PUBLISHED'),
    (903105, 'PdmItem', 'FIsPurchase',      '允许采购', 'BoolValue',          'BOOLEAN', false, NULL,
     '{}'::jsonb, false, 250, 1, 'PUBLISHED'),
    (903106, 'PdmItem', 'FIsInventory',     '允许库存', 'BoolValue',          'BOOLEAN', false, NULL,
     '{}'::jsonb, false, 260, 1, 'PUBLISHED'),
    (903107, 'PdmItem', 'FIsSubContract',   '允许委外', 'BoolValue',          'BOOLEAN', false, NULL,
     '{}'::jsonb, false, 270, 1, 'PUBLISHED'),
    (903108, 'PdmItem', 'FIsSale',          '允许销售', 'BoolValue',          'BOOLEAN', false, NULL,
     '{}'::jsonb, false, 280, 1, 'PUBLISHED'),
    (903109, 'PdmItem', 'F_ZXCV_ZYCS',      '主要参数', 'DefaultObjectValue', 'VARCHAR', false, NULL,
     '{}'::jsonb, true,  290, 1, 'PUBLISHED'),
    (903110, 'PdmItem', 'F_zxcv_SCCS',      '生产厂名', 'DefaultObjectValue', 'VARCHAR', false, NULL,
     '{}'::jsonb, true,  300, 1, 'PUBLISHED'),
    (903111, 'PdmItem', 'F_PJQF_XMDA',      '项目代号', 'DefaultObjectValue', 'VARCHAR', false, NULL,
     '{}'::jsonb, true,  310, 1, 'PUBLISHED'),
    (903112, 'PdmItem', 'F_zxcv_JSCS',      '技术参数', 'DefaultObjectValue', 'VARCHAR', false, NULL,
     '{}'::jsonb, false, 320, 1, 'PUBLISHED')
ON CONFLICT (entity_code, code) DO NOTHING;

-- ---------------------------------------------------------------------
-- 把映射缺口补上取值路径（只改 sourcePath/status/note，其余列不动）
--
-- 物料：14 个缺口里 13 个能落到客制字段；FMATERIALID 是金蝶内码，由 ERP 分配，
--       永远留空（GAP 是对的，不是没做完）。ORIGIN 与 WULIAOSHUXING 同指 FErpClsID。
-- ---------------------------------------------------------------------
WITH src AS (
    SELECT id, rules FROM itg_mapping
     WHERE company_id = 1 AND deleted = false AND object_code = 'Material'
), patched AS (
    SELECT s.id, jsonb_agg(
               CASE e.elem ->> 'pdmName'
                   WHEN 'FMATERIALID' THEN e.elem || '{"note":"金蝶物料内码由 ERP 分配，PLM 侧不保存（下发留空由金蝶生成）"}'::jsonb
                   WHEN 'ORIGIN' THEN e.elem || '{"sourcePath":"ext_json.FErpClsID","status":"OK","note":"旧「来源」并入物料属性；值取 ext_json.FErpClsID"}'::jsonb
                   WHEN 'WULIAOSHUXING' THEN e.elem || '{"sourcePath":"ext_json.FErpClsID","status":"OK","note":"与 ORIGIN 指向同一 ERP 字段，取同一客制字段"}'::jsonb
                   WHEN 'CUNHUOLEIBIE' THEN e.elem || '{"sourcePath":"ext_json.FCategoryID","status":"OK","note":null}'::jsonb
                   WHEN 'JIHUACELUE' THEN e.elem || '{"sourcePath":"ext_json.FPlanningStrategy","status":"OK","note":null}'::jsonb
                   WHEN 'YUNXUSHENGCHAN' THEN e.elem || '{"sourcePath":"ext_json.FIsProduce","status":"OK","note":null}'::jsonb
                   WHEN 'YUNXUCAIGOU' THEN e.elem || '{"sourcePath":"ext_json.FIsPurchase","status":"OK","note":null}'::jsonb
                   WHEN 'YUNXUKUCUN' THEN e.elem || '{"sourcePath":"ext_json.FIsInventory","status":"OK","note":null}'::jsonb
                   WHEN 'YUNXUWEIWAI' THEN e.elem || '{"sourcePath":"ext_json.FIsSubContract","status":"OK","note":null}'::jsonb
                   WHEN 'YUNXUXIAOSHOU' THEN e.elem || '{"sourcePath":"ext_json.FIsSale","status":"OK","note":null}'::jsonb
                   WHEN 'ZYCS' THEN e.elem || '{"sourcePath":"ext_json.F_ZXCV_ZYCS","status":"OK","note":null}'::jsonb
                   WHEN 'MATERIALNAME' THEN e.elem || '{"sourcePath":"ext_json.F_zxcv_SCCS","status":"OK","note":null}'::jsonb
                   WHEN 'XMDH' THEN e.elem || '{"sourcePath":"ext_json.F_PJQF_XMDA","status":"OK","note":null}'::jsonb
                   WHEN 'JSYQ' THEN e.elem || '{"sourcePath":"ext_json.F_zxcv_JSCS","status":"OK","note":null}'::jsonb
                   ELSE e.elem
               END ORDER BY e.ord) AS rules
      FROM src s
      CROSS JOIN LATERAL jsonb_array_elements(s.rules) WITH ORDINALITY AS e(elem, ord)
     GROUP BY s.id
)
UPDATE itg_mapping m SET rules = p.rules, updated_at = now()
  FROM patched p WHERE m.id = p.id;

-- ---------------------------------------------------------------------
-- BOM：4 个缺口分别落到 父项单位 / 项目代号 / 修改人编码 / 子项类型
-- 说明：父项单位、修改人编码、子项类型由 KingdeePayloadService 在源报文里补出，
--       不再占用 meta_field（它们本来就来自既有表列）。
-- ---------------------------------------------------------------------
WITH src AS (
    SELECT id, rules FROM itg_mapping
     WHERE company_id = 1 AND deleted = false AND object_code = 'Bom'
), patched AS (
    SELECT s.id, jsonb_agg(
               CASE
                   WHEN e.elem ->> 'pdmName' = 'UNIT'
                        AND (e.elem ->> 'target') NOT LIKE '[].%'
                       THEN e.elem || '{"sourcePath":"parentUnit","status":"OK","note":"父项单位取自父件 item_master.unit"}'::jsonb
                   WHEN e.elem ->> 'pdmName' = 'XMDH'
                       THEN e.elem || '{"sourcePath":"ext_json.F_PJQF_XMDA","status":"OK","note":"项目代号取父件 ext_json.F_PJQF_XMDA"}'::jsonb
                   WHEN e.elem ->> 'pdmName' = 'UPDATEUID'
                       THEN e.elem || '{"sourcePath":"updatedByCode","status":"OK","note":"修改人编码对应新系统最后修改人账号"}'::jsonb
                   WHEN e.elem ->> 'pdmName' = 'BOMZXLX'
                       THEN e.elem || '{"sourcePath":"lines[].childType","status":"OK","note":"子项类型取子件 itemType"}'::jsonb
                   ELSE e.elem
               END ORDER BY e.ord) AS rules
      FROM src s
      CROSS JOIN LATERAL jsonb_array_elements(s.rules) WITH ORDINALITY AS e(elem, ord)
     GROUP BY s.id
)
UPDATE itg_mapping m SET rules = p.rules, updated_at = now()
  FROM patched p WHERE m.id = p.id;
