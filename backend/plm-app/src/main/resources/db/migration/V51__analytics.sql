-- =====================================================================
-- P10-1 统计看板
--
-- 为什么不复用各域的 read 权限：
--   看板把六个域汇总到一页，若复用 pse.doc.read / chg.ecr.read / ...，
--   就得同时具备六个域的权限才能看完整一页，反而更难授权；
--   且看板只做聚合计数、不下发可编辑明细，单独一个只读权限更贴合意图。
--
-- 授权范围：SYS_ADMIN + BUSINESS（管理视角，与 P9 集成类页面的 read 权限同口径）。
-- =====================================================================

INSERT INTO sys_permission (code, module, action, name, name_en, perm_type) VALUES
    ('ana.dashboard.read', 'ANA', 'READ', '统计看板查看', 'Analytics dashboard read', 'FUNCTION')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission (role_id, permission_code)
SELECT r.id, p.code
FROM sys_role r
JOIN sys_permission p ON p.code = 'ana.dashboard.read'
WHERE r.code IN ('SYS_ADMIN', 'BUSINESS')
ON CONFLICT DO NOTHING;
