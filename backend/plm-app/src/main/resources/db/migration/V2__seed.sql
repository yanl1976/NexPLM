-- =====================================================================
-- 初始数据：默认公司、内置角色、功能权限点、示例生命周期与枚举
-- 说明：管理员账号由 AdminInitializer 在启动时创建（密码需加密，不能用静态 SQL）。
-- =====================================================================

INSERT INTO sys_company (id, code, name, short_name) VALUES (1, 'DEFAULT', '默认公司', '默认')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role (id, company_id, code, name, role_type, data_scope) VALUES
    (10, 1, 'SYS_ADMIN', '系统管理员', 'SYS_ADMIN', 'ALL'),
    (11, 1, 'SEC_ADMIN', '安全保密管理员', 'SEC_ADMIN', 'COMPANY'),
    (12, 1, 'AUDITOR',   '安全审计员',   'AUDITOR',   'COMPANY'),
    (13, 1, 'BUSINESS',  '业务用户',     'BUSINESS',  'COMPANY')
ON CONFLICT DO NOTHING;

INSERT INTO sys_permission (code, module, action, name, name_en, perm_type) VALUES
    ('system.user.manage',  'SYSTEM', 'MANAGE', '用户管理',     'User management',      'FUNCTION'),
    ('system.role.manage',  'SYSTEM', 'MANAGE', '角色与授权',   'Role management',      'FUNCTION'),
    ('system.org.manage',   'SYSTEM', 'MANAGE', '组织管理',     'Organization',         'FUNCTION'),
    ('system.meta.manage',  'SYSTEM', 'MANAGE', '元数据建模',   'Metadata modeling',    'FUNCTION'),
    ('system.audit.read',   'SYSTEM', 'READ',   '审计日志查看', 'Audit log',            'FUNCTION'),
    ('pdm.doc.read',        'PDM',    'READ',   '文档查看',     'Document read',        'FUNCTION'),
    ('pdm.doc.write',       'PDM',    'WRITE',  '文档编辑',     'Document edit',        'FUNCTION'),
    ('pdm.doc.delete',      'PDM',    'DELETE', '文档删除',     'Document delete',      'FUNCTION'),
    ('pdm.doc.release',     'PDM',    'RELEASE','文档发布',     'Document release',     'FUNCTION'),
    ('pse.item.read',       'PSE',    'READ',   '零部件查看',   'Item read',            'FUNCTION'),
    ('pse.item.write',      'PSE',    'WRITE',  '零部件编辑',   'Item edit',            'FUNCTION'),
    ('pse.item.delete',     'PSE',    'DELETE', '零部件删除',   'Item delete',          'FUNCTION'),
    ('change.read',         'CHANGE', 'READ',   '变更查看',     'Change read',          'FUNCTION'),
    ('change.write',        'CHANGE', 'WRITE',  '变更编辑',     'Change edit',          'FUNCTION'),
    ('capp.read',           'CAPP',   'READ',   '工艺查看',     'Process read',         'FUNCTION'),
    ('capp.write',          'CAPP',   'WRITE',  '工艺编辑',     'Process edit',         'FUNCTION'),
    ('project.read',        'PROJECT','READ',   '项目查看',     'Project read',         'FUNCTION'),
    ('project.write',       'PROJECT','WRITE',  '项目编辑',     'Project edit',         'FUNCTION'),
    ('quality.read',        'QUALITY','READ',   '质量查看',     'Quality read',         'FUNCTION'),
    ('quality.write',       'QUALITY','WRITE',  '质量编辑',     'Quality edit',         'FUNCTION'),
    ('file.upload',         'FILE',   'UPLOAD', '文件上传',     'File upload',          'FUNCTION'),
    ('file.download',       'FILE',   'DOWNLOAD','文件下载',    'File download',        'FUNCTION')
ON CONFLICT DO NOTHING;

-- 系统管理员拥有全部功能权限
INSERT INTO sys_role_permission (role_id, permission_code)
SELECT 10, p.code FROM sys_permission p
ON CONFLICT DO NOTHING;

-- 业务用户：默认只读 + 编辑（发布/删除需单独授权）
INSERT INTO sys_role_permission (role_id, permission_code)
SELECT 13, p.code FROM sys_permission p
WHERE p.code IN ('pdm.doc.read', 'pdm.doc.write', 'pse.item.read', 'pse.item.write',
                 'change.read', 'change.write', 'capp.read', 'capp.write',
                 'project.read', 'project.write', 'quality.read', 'quality.write',
                 'file.upload', 'file.download')
ON CONFLICT DO NOTHING;

-- 审计员：只读审计
INSERT INTO sys_role_permission (role_id, permission_code)
SELECT 12, p.code FROM sys_permission p WHERE p.code = 'system.audit.read'
ON CONFLICT DO NOTHING;

-- 安全保密管理员：策略相关
INSERT INTO sys_role_permission (role_id, permission_code)
SELECT 11, p.code FROM sys_permission p
WHERE p.code IN ('system.user.manage', 'system.role.manage', 'system.audit.read')
ON CONFLICT DO NOTHING;

-- 默认生命周期（业务对象主状态机）
INSERT INTO meta_lifecycle (id, code, name, type, definition_json, status) VALUES
(100, 'DEFAULT_LIFECYCLE', '默认生命周期', 'MAIN',
 '{"initial":"Draft","states":[{"code":"Draft","name":"草稿","allFlow":true},
   {"code":"InWorkflow","name":"审批中","allFlow":false},
   {"code":"Released","name":"已发布","allFlow":false},
   {"code":"Obsolete","name":"已废弃","allFlow":true}],
   "transitions":[{"from":"Draft","to":"InWorkflow","action":"submit"},
                  {"from":"InWorkflow","to":"Released","action":"approve"},
                  {"from":"InWorkflow","to":"Draft","action":"reject"},
                  {"from":"Released","to":"Obsolete","action":"obsolete"},
                  {"from":"Obsolete","to":"Draft","action":"revise"}]}'::jsonb,
 'PUBLISHED')
ON CONFLICT DO NOTHING;

-- 示例枚举：文档类型 / 任务状态
INSERT INTO meta_enum (code, name) VALUES
    ('DocType', '文档类型'),
    ('TaskState', '任务状态'),
    ('ItemType', '零部件类型')
ON CONFLICT DO NOTHING;

INSERT INTO meta_enum_item (enum_code, item_code, item_name, sort) VALUES
    ('DocType', 'NORMAL', '普通文档', 10),
    ('DocType', 'CAD',    'CAD 文档', 20),
    ('DocType', 'SPEC',   '技术规范', 30),
    ('TaskState', 'New',       '未接收', 10),
    ('TaskState', 'Executing', '执行中', 20),
    ('TaskState', 'Reviewing', '待审核', 30),
    ('TaskState', 'Finished',  '已完成', 40),
    ('ItemType', 'PRODUCT',  '产品',   10),
    ('ItemType', 'ASSEMBLY', '部件',   20),
    ('ItemType', 'PART',     '零件',   30),
    ('ItemType', 'STANDARD', '标准件', 40),
    ('ItemType', 'TOOL',     '工装',   50),
    ('ItemType', 'GAUGE',    '量具',   60),
    ('ItemType', 'MOLD',     '模具',   70),
    ('ItemType', 'PURCHASE', '外购件', 80)
ON CONFLICT DO NOTHING;
