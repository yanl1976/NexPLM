-- =====================================================================
-- P6-7 典型工艺库（P6 任务 7：典型工艺库与复制）
--
-- 设计取舍：**不另建典型工艺主档表**，改为在 capp_routing 上加 typical 标记：
--   typical = true  → 典型工艺（object_id 为空，不绑定具体零件，可被任意零件套用）
--   typical = false → 零件工艺路线（原有语义：同一零件同时只有一条 RELEASED）
--
-- 理由：典型工艺与零件工艺的差别只在「有没有绑定零件」，工序/工步结构完全一样。
--       复用同一张表后，工序整表替换、工步迁移、序号重建、资源校验、复制等逻辑零改动复用；
--       而「另存为典型」「套用到零件」本质都是跨对象的深拷贝，也不需要第二套工序表。
-- =====================================================================

ALTER TABLE capp_routing ALTER COLUMN object_id DROP NOT NULL;
ALTER TABLE capp_routing ADD COLUMN typical boolean NOT NULL DEFAULT false;

COMMENT ON COLUMN capp_routing.typical IS '是否典型工艺：true=通用工艺（不绑零件），false=零件工艺路线';
COMMENT ON COLUMN capp_routing.object_id IS '零件 plm_object.id；typical=true 时为空';

-- 典型工艺：同公司内名称唯一（便于按名称检索/套用；软删除不占用名额，允许同名重建）
CREATE UNIQUE INDEX uk_capp_routing_typical_name
    ON capp_routing (company_id, name)
    WHERE deleted = false AND typical = true;

-- 典型工艺列表：按「公司 + 类型 + 状态」翻页，与零件路线列表的索引分开走
CREATE INDEX idx_capp_routing_typical
    ON capp_routing (company_id, typical, status, updated_at DESC)
    WHERE deleted = false;
