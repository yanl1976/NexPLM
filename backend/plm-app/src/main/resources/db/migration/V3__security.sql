-- 安全模块新增权限点（规则权限、对象 ACL、生命周期管理），并授予系统管理员

INSERT INTO sys_permission (code, module, action, name, name_en, perm_type) VALUES
    ('system.rule.manage',      'SYSTEM', 'MANAGE', '规则权限管理', 'Permission rules', 'FUNCTION'),
    ('system.acl.manage',       'SYSTEM', 'MANAGE', '对象授权管理', 'Object ACL',       'FUNCTION'),
    ('system.lifecycle.manage', 'SYSTEM', 'MANAGE', '生命周期管理', 'Lifecycle',        'FUNCTION')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission (role_id, permission_code)
SELECT 10, p.code FROM sys_permission p
WHERE p.code IN ('system.rule.manage', 'system.acl.manage', 'system.lifecycle.manage')
ON CONFLICT DO NOTHING;

-- 规则权限示例（可直接在界面调整）：已发布对象允许所有用户读取
INSERT INTO sys_permission_rule (id, code, name, object_type, expr_json, actions, priority, effect, enabled, remark)
VALUES (200, 'RELEASED_READ_ALL', '已发布对象全员可读', 'PdmDoc',
        '{"op":"EQ","field":"lifecycleState","value":"Released"}'::jsonb,
        'READ', 100, 'ALLOW', true, '示例规则：可按业务调整')
ON CONFLICT DO NOTHING;
