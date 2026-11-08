-- =====================================================================
-- 密码策略（docs/07 待整改 ④）：历史密码不可重用
--
-- 现状：密码只有长度 8~64（IamDtos 的 @Size），无复杂度、无历史、无有效期；
--   也没有"用户自助改密"接口（只有管理员重置）。本迁移只加历史表，
--   策略与接口在代码侧（PasswordPolicy + POST /auth/change-password）。
--
-- 说明：只存 bcrypt 哈希，用于"新密码不得与最近 N 次相同"的比对；
--   N 由 nexplm.security.password.history-size 控制（默认 3，0=不检查）。
-- =====================================================================

CREATE TABLE IF NOT EXISTS sys_user_pwd_history (
    id            bigint       PRIMARY KEY,
    user_id       bigint       NOT NULL,
    password_hash varchar(100) NOT NULL,
    created_at    timestamptz  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_sys_user_pwd_history
    ON sys_user_pwd_history (user_id, created_at DESC);
