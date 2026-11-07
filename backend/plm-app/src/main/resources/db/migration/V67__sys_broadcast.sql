-- =====================================================================
-- 批次 F-2 系统广播（登录后弹窗）
--
-- 旧系统模型（docs/06 EXP-20261107-16 第 4 条）：
--   BROADCAST.schema 走通用 ENTITY（EID/ENAME/ECODE/STATEID/EVERSION…），
--   业务字段只有 BROADCASTCONTENT STRING1000 + RELEASESTATUS/RELEASETIME；
--   目标对象靠通用关联表 BROADCASTLINK（PEID→CEID）维护，菜单操作走
--   CreateObject/EditObject 并带 SendMessage=true 推送站内消息；
--   列表拆成「预约广播 BroadcastReservation」与「广播查询 BroadcastQuery」两页。
--
-- 新系统取舍：
--   1. 不复刻"广播=通用对象 + 关联表"——通用对象模型对本系统是纯负担，
--      这里用专用表，目标范围先只做"全公司"，不做按角色/组织的关联表（遗留）。
--   2. 「预约广播 / 广播查询」合并为一页，靠 publish_state + publish_at 区分，
--      不照搬两个菜单（同一份数据的两种视图不值得两个入口）。
--   3. 已读按收件人维度（sys_broadcast_read），沿用批次 E-2 的收件人思维——
--      旧的 BROADCAST 只有行级状态，谁都可能把别人的通知标成已读。
-- =====================================================================

CREATE TABLE sys_broadcast (
    id            bigserial    PRIMARY KEY,
    company_id    bigint       NOT NULL,
    title         varchar(200) NOT NULL,
    content       varchar(2000) NOT NULL,
    -- NORMAL 普通 / IMPORTANT 重要 / URGENT 紧急（前端据此配色）
    level         varchar(16)  NOT NULL DEFAULT 'NORMAL',
    -- DRAFT 草稿 / PUBLISHED 已发布 / REVOKED 已撤回
    publish_state varchar(16)  NOT NULL DEFAULT 'DRAFT',
    -- 预约发布时间：为空表示立即生效；publish_at 在未来时不出现在弹窗里
    publish_at    timestamptz,
    expire_at     timestamptz,
    -- 是否登录后弹窗（对齐旧系统菜单操作里的 SendMessage 语义）
    popup         boolean      NOT NULL DEFAULT true,
    created_by    bigint,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    updated_at    timestamptz  NOT NULL DEFAULT now()
);

CREATE INDEX idx_sys_broadcast_state
    ON sys_broadcast (company_id, publish_state, publish_at DESC);

CREATE TABLE sys_broadcast_read (
    broadcast_id bigint      NOT NULL,
    user_id      bigint      NOT NULL,
    read_at      timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (broadcast_id, user_id)
);

INSERT INTO sys_permission (code, module, action, name, name_en, perm_type) VALUES
    ('sys.broadcast.read',  'IAM', 'READ',  '系统广播查看', 'System broadcast read',  'FUNCTION'),
    ('sys.broadcast.write', 'IAM', 'WRITE', '系统广播发布', 'System broadcast write', 'FUNCTION')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission (role_id, permission_code)
SELECT r.id, p.code
FROM sys_role r
JOIN sys_permission p ON p.code IN ('sys.broadcast.read', 'sys.broadcast.write')
WHERE r.code = 'SYS_ADMIN'
ON CONFLICT DO NOTHING;
