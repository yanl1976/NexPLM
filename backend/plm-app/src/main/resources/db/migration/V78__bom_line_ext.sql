-- =====================================================================
-- BOM 行字段扩展（T-CORE-03）
--
-- 背景（docs/15 3.3）：蓝本 PSEBOMLINE 37 列，新系统 bom_line 18 列。
--   `qty` 对应蓝本 `USEQTY`（单台用量）；蓝本另有 `TOTALQTY`（总用量）、
--   `SURPLUSQTY`（剩余）、`SUBUSEQTY`（子用量），以及 `LBOMTYPE`（BOM 类型）、
--   `ITEMROUTE`（工序路线）、`DATASOURCE`（数据来源）。
--
-- 决策：
--   1. **不改名 `qty`**——它是单台用量，改名会牵动全部 SQL 与前端；
--      新增三列承载蓝本语义并在注释里写清对应关系；
--   2. `total_qty` 允许为空：为空表示"按层级累乘计算"（`summary` 已有此能力），
--      填了则以填的为准（导入历史数据时用）；
--   3. `bom_type` 默认 `DESIGN`；蓝本实测该库只有 `Quote`（报价 BOM），
--      迁移时映射成 `QUOTE` 即可，不新增制造/装配分支；
--   4. `data_source` 记录来源（手工/导入/ERP 回写），便于排查。
-- =====================================================================

ALTER TABLE bom_line
    ADD COLUMN IF NOT EXISTS total_qty   numeric(18, 6),
    ADD COLUMN IF NOT EXISTS surplus_qty numeric(18, 6),
    ADD COLUMN IF NOT EXISTS sub_use_qty numeric(18, 6),
    ADD COLUMN IF NOT EXISTS bom_type    varchar(32) NOT NULL DEFAULT 'DESIGN',
    ADD COLUMN IF NOT EXISTS item_route  varchar(64),
    ADD COLUMN IF NOT EXISTS data_source varchar(32);

COMMENT ON COLUMN bom_line.qty IS '单台用量（对应蓝本 PSEBOMLINE.USEQTY）';
COMMENT ON COLUMN bom_line.total_qty IS '总用量（对应蓝本 TOTALQTY）；为空表示按层级累乘计算';
COMMENT ON COLUMN bom_line.surplus_qty IS '剩余用量（对应蓝本 SURPLUSQTY）';
COMMENT ON COLUMN bom_line.sub_use_qty IS '子用量（对应蓝本 SUBUSEQTY）';
COMMENT ON COLUMN bom_line.bom_type IS 'BOM 类型：DESIGN/QUOTE/…（对应蓝本 LBOMTYPE）';
COMMENT ON COLUMN bom_line.item_route IS '工序路线（对应蓝本 ITEMROUTE）';
COMMENT ON COLUMN bom_line.data_source IS '数据来源：MANUAL/IMPORT/ERP（对应蓝本 DATASOURCE）';
