-- =====================================================================
-- 批次 E-1 供应商协同（多轮回复 / 已读 / 重要 / 生效失效 / 明细确认）
--
-- 背景（取证见 docs/06 EXP-20261107-15）：
--   旧系统「数据协同」不是站内留言，而是主机厂与供应商两台服务器之间的数据同步；
--   页面上的「回复」实际是**再建一条 DISTRIBUTE**，原说明被拼成文本快照塞进 EXPLAIN，
--   父子之间没有结构化关联 —— 因此无法按单查询往来、无法统计回复率、无法逐条确认。
--   本批不照搬这条路径，改为独立回复表（一张发放单下多条回复，可追溯、可统计）。
--
-- 为什么允许 RECEIVED 之后继续回复：
--   旧系统「我回复的数据」里有 AnswerAgain（CoordinationMenu.menu:59-64），
--   供应商拿到新图纸后常需要二次确认/改承诺交期；一次性回执不满足这个场景。
--
-- 为什么 pull_state / reply_state 放在明细行：
--   旧系统只有附件级 PULLSTATE（COORDINATIONFILE.schema:91），没有「哪一条零件确认了」；
--   供应商实际会逐条确认图纸，故把确认粒度下沉到明细行（PENDING/CONFIRMED/REJECTED）。
--
-- 生效/失效为什么用 timestamptz 而不是旧系统的字符串字段：
--   旧库 EFFECTEDTIME/EXPIRATIONTIME 是 STRING100，比较靠 JS 字符串（CoordinationManager.js:256-259），
--   绕过前端直接调接口就失效。新库用真时间类型，服务端在 send/reply/receipt 统一校验。
-- =====================================================================

ALTER TABLE sup_issuance ADD COLUMN IF NOT EXISTS read_at        timestamptz;
ALTER TABLE sup_issuance ADD COLUMN IF NOT EXISTS is_important   boolean NOT NULL DEFAULT false;
ALTER TABLE sup_issuance ADD COLUMN IF NOT EXISTS effective_at   timestamptz;
ALTER TABLE sup_issuance ADD COLUMN IF NOT EXISTS expire_at      timestamptz;
ALTER TABLE sup_issuance ADD COLUMN IF NOT EXISTS supplier_group varchar(100);

ALTER TABLE sup_issuance_item ADD COLUMN IF NOT EXISTS pull_state  varchar(16) NOT NULL DEFAULT 'UNPULL';
ALTER TABLE sup_issuance_item ADD COLUMN IF NOT EXISTS reply_state varchar(16) NOT NULL DEFAULT 'PENDING';

CREATE INDEX IF NOT EXISTS idx_sup_issuance_expire
    ON sup_issuance (company_id, expire_at) WHERE deleted = false;

CREATE TABLE IF NOT EXISTS sup_issuance_reply (
    id                  bigint       PRIMARY KEY,
    company_id          bigint       NOT NULL,
    issuance_id         bigint       NOT NULL,
    -- ACK 已收阅 / CONFIRM 图纸确认 / COMMIT_DATE 承诺交期 / REJECT 无法满足
    reply_type          varchar(16)  NOT NULL DEFAULT 'ACK',
    content             varchar(2000),
    promised_date       date,
    -- 附件走 file 中心（file_id + 文件名快照），与回执同一套做法
    attachment_file_ids jsonb        NOT NULL DEFAULT '[]'::jsonb,
    attachment_names    jsonb        NOT NULL DEFAULT '[]'::jsonb,
    replied_by          bigint,
    replied_at          timestamptz  NOT NULL DEFAULT now(),
    deleted             boolean      NOT NULL DEFAULT false
);

CREATE INDEX IF NOT EXISTS idx_sup_issuance_reply
    ON sup_issuance_reply (issuance_id, replied_at) WHERE deleted = false;

INSERT INTO sys_permission (code, module, action, name, name_en, perm_type) VALUES
    ('sup.coordination.read',  'SUP', 'READ',  '供应商协同回复查看', 'Supplier coordination read',  'FUNCTION'),
    ('sup.coordination.write', 'SUP', 'WRITE', '供应商协同回复登记', 'Supplier coordination write', 'FUNCTION')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission (role_id, permission_code)
SELECT r.id, p.code
FROM sys_role r
JOIN sys_permission p ON p.code IN ('sup.coordination.read', 'sup.coordination.write')
WHERE r.code IN ('SYS_ADMIN', 'BUSINESS')
ON CONFLICT DO NOTHING;
