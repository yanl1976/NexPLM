-- =====================================================================
-- 批次 C-4｜工序挂工装
--
-- 旧系统取证（PLMSOFT/Server/InforCenter/CAPP/Tooling）：
--   WorlflowPlaceOnResourcePage（把工装放到资源/工序上）
--   ToolingImpactAnalysis（工装被工序引用后的影响分析）
-- → 工序行增加 tooling_id 引用；工装被引用后禁止删除（影响分析可查引用清单）。
--
-- 设计取舍：与 resource_id 同构，一道工序挂一件工装（单引用）。
-- 一件工装可被多条路线的多道工序引用；如后续需要「一道工序挂多件工装」，
-- 再抽 capp_routing_step_tooling 关系表，届时本列可平滑降级为「主工装」。
-- =====================================================================

ALTER TABLE capp_routing_step ADD COLUMN IF NOT EXISTS tooling_id bigint;

CREATE INDEX IF NOT EXISTS idx_capp_step_tooling ON capp_routing_step (tooling_id);
