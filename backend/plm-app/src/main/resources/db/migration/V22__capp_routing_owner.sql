-- =====================================================================
-- P6-2 工艺分工：工艺路线主档增加「工艺负责人」（工艺员/分工到人）
--
-- 工序级的车间分工仍用 capp_routing_step.work_center（P6-3 资源库落地后改为资源引用）；
-- 本迁移只补「这条路线归谁维护」这一层，默认取创建人。
-- =====================================================================

ALTER TABLE capp_routing ADD COLUMN IF NOT EXISTS owner_id bigint;
CREATE INDEX IF NOT EXISTS idx_capp_routing_owner ON capp_routing (owner_id);
