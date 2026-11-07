-- =====================================================================
-- 批次 F-3 系统运行日志页（登录日志 + 操作审计日志）
--
-- 取证见 docs/06 EXP-20261107-16 第 3、8 条：
--   旧系统有两套日志——业务操作日志 OPERATELOG（InforCenter/ThreeManagement，
--   USERID/OBJNAME/OBJCODE/OBJTYPE/OBJID/OPERATE/DESCRIPTION + 三个索引，
--   就是"审计检索页"的数据源）与 Platform/AccessRecord；且 OperateLogSearch 模块
--   被列入「不能在功能授权中授权的模块」（ThreeRole.setting:4）。
--   新系统 sys_audit_log / sys_login_log 两张表早就存在，但没有任何查询接口与页面
--   （前端 api/audit.ts 是 IATF16949 体系审核，与此无关）。本批只补查询。
--
-- 为什么要独立权限码而不是复用 sys.user.read：
--   日志是审计证据，能看"谁在什么时候删了什么"本身是高权限能力；
--   旧系统明确禁止通过普通功能授权把它发出去，故单列 sys.log.read，
--   只给系统管理员与安全审计员（AUDITOR，V2 已内置的三角色之一）。
-- =====================================================================

INSERT INTO sys_permission (code, module, action, name, name_en, perm_type) VALUES
    ('sys.log.read', 'IAM', 'READ', '系统运行日志查看', 'System log read', 'FUNCTION')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission (role_id, permission_code)
SELECT r.id, p.code
FROM sys_role r
JOIN sys_permission p ON p.code = 'sys.log.read'
WHERE r.code IN ('SYS_ADMIN', 'AUDITOR')
ON CONFLICT DO NOTHING;

-- 在线用户（F-1）也归审计员：审计员需要看"谁在线、谁被踢下线"，但不得踢人，
-- 故只补 read，不补 write。
INSERT INTO sys_role_permission (role_id, permission_code)
SELECT r.id, p.code
FROM sys_role r
JOIN sys_permission p ON p.code = 'sys.session.read'
WHERE r.code = 'AUDITOR'
ON CONFLICT DO NOTHING;
