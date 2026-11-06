-- =====================================================================
-- P4 工作流：业务绑定、业务对象 ↔ 流程实例关联、审批意见
--
-- 说明：
--   1. 流程引擎自身的 ACT_* 表由 flowable.database-schema-update 管理，
--      **不纳入 Flyway**（引擎升级时由其自带脚本演进，混入 Flyway 会与升级冲突）；
--   2. 本迁移只建 PLM 侧需长期留存、且要参与业务查询与审计的表；
--   3. 电子签名、委托在后续迁移（V16）补充，避免一次性引入未使用的表。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 业务绑定：按对象类型（可选类别）决定用哪条流程；同类型可配多条按 priority 取首条
-- condition_json 预留条件判定（如 {"lifecycleState":"Draft"}），当前仅存不判
-- ---------------------------------------------------------------------
CREATE TABLE wf_binding (
    id             bigint       PRIMARY KEY,
    object_type    varchar(64)  NOT NULL,
    category       varchar(64),
    condition_json jsonb        NOT NULL DEFAULT '{}'::jsonb,
    process_key    varchar(128) NOT NULL,
    name           varchar(200),
    priority       int          NOT NULL DEFAULT 10,
    enabled        boolean      NOT NULL DEFAULT true,
    remark         varchar(500),
    created_at     timestamptz  NOT NULL DEFAULT now(),
    updated_at     timestamptz  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_wf_binding ON wf_binding (object_type, COALESCE(category, ''), process_key);
CREATE INDEX idx_wf_binding_type ON wf_binding (object_type, priority);

-- ---------------------------------------------------------------------
-- 业务对象与流程实例的关联：一个对象可多次发起，取 started_at 最新的一条为「当前流程」
-- status 仅作快速筛选用，权威状态由运行时引擎判定后回写
-- ---------------------------------------------------------------------
CREATE TABLE wf_instance_link (
    id                  bigint       PRIMARY KEY,
    object_type         varchar(64)  NOT NULL,
    object_id           bigint       NOT NULL,
    process_instance_id varchar(64)  NOT NULL,
    process_key         varchar(128) NOT NULL,
    business_key        varchar(200),
    status              varchar(16)  NOT NULL DEFAULT 'RUNNING',
    started_by          bigint       NOT NULL,
    started_at          timestamptz  NOT NULL DEFAULT now(),
    ended_at            timestamptz
);
CREATE UNIQUE INDEX uk_wf_link_instance ON wf_instance_link (process_instance_id);
CREATE INDEX idx_wf_link_object ON wf_instance_link (object_type, object_id, started_at DESC);
CREATE INDEX idx_wf_link_status ON wf_instance_link (status);
CREATE INDEX idx_wf_link_starter ON wf_instance_link (started_by, started_at DESC);

-- ---------------------------------------------------------------------
-- 审批意见：每次办理（通过/驳回/撤回/转办）留一条，与 Flowable 注释互补
-- action 取值：START / APPROVE / REJECT / WITHDRAW / DELEGATE / COMMENT
-- ---------------------------------------------------------------------
CREATE TABLE wf_comment (
    id                  bigint        PRIMARY KEY,
    process_instance_id varchar(64)   NOT NULL,
    task_id             varchar(64),
    task_key            varchar(128),
    action              varchar(32)   NOT NULL,
    comment_text        varchar(2000),
    user_id             bigint,
    created_at          timestamptz   NOT NULL DEFAULT now()
);
CREATE INDEX idx_wf_comment_instance ON wf_comment (process_instance_id, created_at);

-- ---------------------------------------------------------------------
-- 权限点：流程发起 / 待办查看 / 任务办理 / 流程运维
-- ---------------------------------------------------------------------
INSERT INTO sys_permission (code, module, action, name, name_en, perm_type) VALUES
    ('wf.process.start',  'WF', 'START',  '发起流程',   'Start process',   'FUNCTION'),
    ('wf.task.read',      'WF', 'READ',   '待办查看',   'Task read',       'FUNCTION'),
    ('wf.task.audit',     'WF', 'AUDIT',  '任务办理',   'Task audit',      'FUNCTION'),
    ('wf.process.manage', 'WF', 'MANAGE', '流程运维',   'Process manage',  'FUNCTION')
ON CONFLICT DO NOTHING;

-- 系统管理员拥有全部工作流权限
INSERT INTO sys_role_permission (role_id, permission_code)
SELECT 10, p.code FROM sys_permission p WHERE p.code LIKE 'wf.%'
ON CONFLICT DO NOTHING;

-- 业务用户：可发起、看待办、办理任务（运维权限不给）
INSERT INTO sys_role_permission (role_id, permission_code)
SELECT 13, p.code FROM sys_permission p
WHERE p.code IN ('wf.process.start', 'wf.task.read', 'wf.task.audit')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- 示例绑定：文档与零部件默认走通用审批流程（可在界面调整）
-- ---------------------------------------------------------------------
INSERT INTO wf_binding (id, object_type, category, condition_json, process_key, name, priority, enabled) VALUES
    (150001, 'PdmDoc',  NULL, '{}'::jsonb, 'nexplmApproval', '文档通用审批',   10, true),
    (150002, 'PdmItem', NULL, '{}'::jsonb, 'nexplmApproval', '零部件通用审批', 10, true)
ON CONFLICT DO NOTHING;
