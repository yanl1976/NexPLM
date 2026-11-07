-- =====================================================================
-- 批次 F-1 在线用户与强制下线
--
-- 为什么需要新表（取证见 docs/06 EXP-20261107-16 第 1、2 条）：
--   旧系统在线用户是服务端内存会话表，列表由 WebService GetOnlineUserGridData 提供，
--   踢人按 UserID + LoginDeviceType + LoginID + LoginIP 精确定位到「某台设备的某个会话」。
--   新系统是纯无状态 JWT（JwtService 只带 username/companyId/typ），既没有在线列表，
--   也无法让一个已签发的 token 提前失效 —— 强制下线根本做不到。
--
-- 修法：登录成功即落一条会话记录（id 同时作为 JWT 的 sid 声明），认证过滤器校验会话
--       是否 active；踢人 = 把会话置失效，此后该 token 立刻 401。
--       设备类型保留 PC/MOBILE（旧系统 OnlineUserType 的等价物，ICP8.3.10 支持分开踢）。
-- =====================================================================

CREATE TABLE sys_user_session (
    id             bigint       PRIMARY KEY,
    company_id     bigint       NOT NULL,
    user_id        bigint       NOT NULL,
    username       varchar(64)  NOT NULL,
    -- PC / MOBILE / SSO（旧系统按 OnlineUserType 分 PC 与移动端退出）
    device_type    varchar(16)  NOT NULL DEFAULT 'PC',
    ip             varchar(64),
    ua             varchar(500),
    login_at       timestamptz  NOT NULL DEFAULT now(),
    last_active_at timestamptz  NOT NULL DEFAULT now(),
    ended_at       timestamptz,
    -- LOGOUT 主动登出 / KICKED 被踢 / KICK_ALL 全部下线 / EXPIRED 过期清理
    end_reason     varchar(32),
    active         boolean      NOT NULL DEFAULT true
);

CREATE INDEX idx_sys_user_session_active
    ON sys_user_session (company_id, active, last_active_at DESC);
CREATE INDEX idx_sys_user_session_user
    ON sys_user_session (user_id, active);

INSERT INTO sys_permission (code, module, action, name, name_en, perm_type) VALUES
    ('sys.session.read',  'IAM', 'READ',  '在线用户查看', 'Online session read',  'FUNCTION'),
    ('sys.session.write', 'IAM', 'WRITE', '在线用户强制下线', 'Online session write', 'FUNCTION')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission (role_id, permission_code)
SELECT r.id, p.code
FROM sys_role r
JOIN sys_permission p ON p.code IN ('sys.session.read', 'sys.session.write')
WHERE r.code = 'SYS_ADMIN'
ON CONFLICT DO NOTHING;
