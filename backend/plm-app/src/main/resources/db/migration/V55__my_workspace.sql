-- =====================================================================
-- 批次 A｜个人工作区收口（对齐旧系统 000 个人工作区 / 001 常用功能）
--
-- 旧系统对应物（见 docs/11 第 5 节）：
--   我的检出  ← CHECKOUTOBJECTVIEW.view（列 OBJTYPE/ENAME/CHECKOUTDATE，OrderBy CHECKOUTDATE DESC）
--   收藏夹    ← Platform/Favorite + FAVORITELINK（CEID/CETYPE 收藏对象、PEID/PETYPE 父级、LSEQUENCE 排序）
--   最近访问  ← Platform/AccessRecord（AccessRecordService 分页取数，含显示模式切换）
--   站内消息  ← InternalMail（本方案复用 sys_notify_message 作为收件箱）
--
-- 为何在平台层建表而不是复用 ws_favorite：
--   ws_favorite 绑定「工作空间文件夹」，是文档库内部的概念；
--   旧系统的收藏是**跨对象类型**的（文档/零部件/项目/工艺卡都能收），
--   两者粒度不同，混用会让收藏夹永远只出现在工作空间里。
-- =====================================================================

CREATE TABLE plm_favorite (
    id          bigint       PRIMARY KEY,
    company_id  bigint       NOT NULL,
    user_id     bigint       NOT NULL,
    object_type varchar(64)  NOT NULL,
    object_id   bigint       NOT NULL,
    -- 收藏夹分组（旧系统靠 FAVORITELINK 的父级 PEID/LPATH 表达层级，
    -- 这里先收敛成单层分组名，避免把树结构的复杂度提前引入）
    folder      varchar(100),
    seq_no      int          NOT NULL DEFAULT 10,
    remark      varchar(500),
    created_at  timestamptz  NOT NULL DEFAULT now()
);

-- 同一用户对同一对象只收藏一次（重复收藏是"幂等成功"，不是 409）
CREATE UNIQUE INDEX uk_plm_favorite
    ON plm_favorite (user_id, object_type, object_id);
CREATE INDEX idx_plm_favorite_user
    ON plm_favorite (user_id, created_at DESC);

CREATE TABLE plm_access_record (
    id          bigint       PRIMARY KEY,
    company_id  bigint       NOT NULL,
    user_id     bigint       NOT NULL,
    object_type varchar(64)  NOT NULL,
    object_id   bigint       NOT NULL,
    -- VIEW=打开详情 / EDIT=进入编辑 / DOWNLOAD=下载主文件 / CHECKOUT=检出
    access_type varchar(16)  NOT NULL DEFAULT 'VIEW',
    accessed_at timestamptz  NOT NULL DEFAULT now()
);

CREATE INDEX idx_plm_access_record_user
    ON plm_access_record (user_id, accessed_at DESC);

-- 站内消息收件箱：sys_notify_message 是"发出去的通知台账"，
-- 个人收件箱需要"读没读"这一维，故补一列而不是另建消息表（同一份数据两种视角）。
ALTER TABLE sys_notify_message ADD COLUMN read_at timestamptz;
CREATE INDEX idx_sys_notify_message_unread
    ON sys_notify_message (company_id, read_at) WHERE read_at IS NULL;

-- 说明：个人工作区**不新增功能权限点**。
-- 旧系统的"我的检出/收藏/访问记录"对所有登录用户开放，数据范围由当前用户自身限定；
-- 若再加一层功能权限，反而会出现"能登录却看不到自己的收藏"的怪现象。
