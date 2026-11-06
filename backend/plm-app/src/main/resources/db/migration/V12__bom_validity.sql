-- ---------------------------------------------------------------------
-- BOM 行有效性区间。
-- 旧系统两套有效性：规则型（PSEBOMLINE.LVALIDITYID → PSEVALIDITY/PSEVALIDITYDATA，
-- STARTDATA/ENDDATA 按 @rulevalidityvalue 过滤）与对象级区间（DATEVALIDITY/UNITVALIDITY）。
-- 新模型收敛为 BOM 行自带日期区间：NULL 表示该侧不限（无区间 = 始终有效），
-- 展开时按 effectAt 过滤 effect_from <= effectAt <= effect_to。
-- ---------------------------------------------------------------------
ALTER TABLE bom_line ADD COLUMN effect_from date;
ALTER TABLE bom_line ADD COLUMN effect_to   date;

CREATE INDEX idx_bom_line_effect ON bom_line (parent_object_id, view_type, effect_from, effect_to);
