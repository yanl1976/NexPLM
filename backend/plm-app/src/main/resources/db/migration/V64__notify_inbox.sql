-- =====================================================================
-- 批次 E-2 站内消息「收件人化」
--
-- 缺陷（取证见 docs/06 EXP-20261107-15 第 10 条）：
--   sys_notify_message 只有 company_id，没有收件人；已读是消息行级单列 read_at。
--   结果全公司共用一个收件箱、任何人都能把别人的消息标记已读，
--   也无法表达「你读了我没读」——这与旧系统 INSTANTMESSAGE.MFROM/MTO 按收件人
--   + INTERNALMAIL.ACTUALRECIVER 过滤的做法不一致。
--
-- 修法：不动 sys_notify_message（它继续承担"消息快照 + 外部通道发件箱"的职责），
--       新增 sys_notify_inbox 作为「每人一份」的收件状态（read_at 落到收件人维度）。
--       同一条消息要给多人时，消息只有一行，收件箱多行，符合"消息是事实、已读是关系"。
--
-- 同时放开通道类型：旧系统 DefaultAlertType=InternalMail（站内是默认通道），
-- 新系统原先 CHECK 只允许 EMAIL/WECOM/DINGTALK，等于"不配外部通道就没有站内消息"。
-- 新增 INTERNAL 类型，由 NotifyService 幂等创建，用于承载站内投递。
-- =====================================================================

ALTER TABLE sys_notify_channel DROP CONSTRAINT IF EXISTS ck_sys_notify_channel_type;
ALTER TABLE sys_notify_channel
    ADD CONSTRAINT ck_sys_notify_channel_type
    CHECK (type IN ('EMAIL', 'WECOM', 'DINGTALK', 'INTERNAL'));

CREATE TABLE IF NOT EXISTS sys_notify_inbox (
    id         bigint      PRIMARY KEY,
    company_id bigint      NOT NULL,
    message_id bigint      NOT NULL,
    user_id    bigint      NOT NULL,
    read_at    timestamptz,
    deleted    boolean     NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_notify_inbox
    ON sys_notify_inbox (message_id, user_id);
CREATE INDEX IF NOT EXISTS idx_sys_notify_inbox_user
    ON sys_notify_inbox (company_id, user_id, read_at) WHERE deleted = false;

-- 历史消息不回填：旧消息没有收件人字段，无法可靠推断该给谁；
-- 强行按公司回填会让"所有人看到所有人的消息"这个缺陷变成持久数据。
-- 本库为开发/测试库，丢弃旧站内消息可接受；上线前如需保留历史，需业务给出收件人口径。
