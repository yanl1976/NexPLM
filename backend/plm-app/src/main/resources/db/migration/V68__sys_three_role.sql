-- =====================================================================
-- 批次 F-4 三员管理（系统管理员 / 安全保密管理员 / 安全审计员）
--
-- 旧系统取证见 docs/06 EXP-20261107-16 第 5~7 条：
--   三员是独立子系统，有独立账号表 THREEROLEUSERINFO（含 ISLOCK/ISFIRSTLOGIN/
--   LASTCHANGEPASSWORDDATE/ISEXCEPTIONLOCK…）、登录时段限制 THREEROLELOGINLIMIT
--   （配合 ThreeRoleAllowWeek 周几 + ThreeRoleAllowTime 1~24 点）、
--   THREEROLEACCESSTOKEN 等；靠 USERINFO.USERROLETYPE（NormalUserType /
--   ThreeRoleType）与普通账号区分，组织树只查 NormalUserType；
--   ThreeRole.setting:4 明确「三员相关模块不能通过普通功能授权授予」
--   （ModulePermissionDisableNav=Organization;ModulePermission;ActionRule;OperateLogSearch）。
--
-- 本批落地范围（用户已确认"先出设计再落地"，不做整套三员产品）：
--   1. 三角色沿用 V2 种子里的 SYS_ADMIN / SEC_ADMIN / AUDITOR（不新建账号表）；
--   2. 新增「同一账号最多持有一个三员角色」的硬约束（代码在 UserAdminService，
--      历史脏数据由 /sys/three-roles 的 violations 暴露出来，便于清理）；
--   3. 三员模块用独立权限码 sys.three-role.read/write，且 write 只能留在 SYS_ADMIN 手上
--      （RoleService.grant 里拦掉把 write 发给别的角色的尝试），对齐旧系统"不能普通授权"；
--   4. 三员相关写操作沿用 @Audit 落 sys_audit_log（USER 增改/启停/改密本来就有）。
--
-- 不做（遗留，需业务先定策略值）：独立三员账号表、登录时段限制、密码复杂度、
-- 首次登录强制改密（新系统已有 must_change_pwd 但未用于三员）、异常登录锁定计数。
-- =====================================================================

INSERT INTO sys_permission (code, module, action, name, name_en, perm_type) VALUES
    ('sys.three-role.read',  'IAM', 'READ',  '三员配置查看', 'Three-role read',  'FUNCTION'),
    ('sys.three-role.write', 'IAM', 'WRITE', '三员配置维护', 'Three-role write', 'FUNCTION')
ON CONFLICT DO NOTHING;

-- write 只给系统管理员；read 另给审计员（审计员要看"谁被设成了三员"）
INSERT INTO sys_role_permission (role_id, permission_code)
SELECT r.id, p.code
FROM sys_role r
JOIN sys_permission p ON p.code = 'sys.three-role.write'
WHERE r.code = 'SYS_ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission (role_id, permission_code)
SELECT r.id, p.code
FROM sys_role r
JOIN sys_permission p ON p.code = 'sys.three-role.read'
WHERE r.code IN ('SYS_ADMIN', 'AUDITOR')
ON CONFLICT DO NOTHING;
