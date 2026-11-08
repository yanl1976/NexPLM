-- =====================================================================
-- 编码管理（批次 B 收尾）｜编码规则接口的权限与审计
--
-- 背景（EXP-20261108-15）：
--   1. `CodeRuleController` 原先没有任何 `@PreAuthorize`：任何登录用户都能改段定义、
--      重置/跳号流水（直接影响后续所有取号），也没有 `@Audit` 留痕；
--   2. 蓝本编码模块本身带权限：`InforCenter/Coding/Config` 下的 `CodeKindManagement.js`
--      提供「编码种类权限」（`GetCodeKindPermissionGridData` / `GetCodeKindUserPermissionInfo`
--      / `GetCodeKindObjectInfoTypePermissionInfo` / `GetCodeKindList`），按用户与对象类型授权。
--   新系统先收敛为功能权限点（查看 / 维护）；「按编码种类/对象类型细分授权」留作 GAP，
--   见 docs/03 B20。
-- =====================================================================

INSERT INTO sys_permission (code, module, action, name, name_en, perm_type) VALUES
    ('system.coding.read',   'SYSTEM', 'READ',   '编码规则查看', 'Coding rule read',   'FUNCTION'),
    ('system.coding.manage', 'SYSTEM', 'MANAGE', '编码规则维护', 'Coding rule manage', 'FUNCTION')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission (role_id, permission_code)
SELECT r.id, p.code
  FROM sys_role r
  JOIN sys_permission p ON p.code IN ('system.coding.read', 'system.coding.manage')
 WHERE r.code = 'SYS_ADMIN'
ON CONFLICT DO NOTHING;
