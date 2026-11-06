-- =====================================================================
-- P8-5-3 供应商数据发放（打包导出 / 回执协同）
--
-- 为什么要"发放单"而不是"选几个零件直接打包下载"：
--   发给供应商的是受控数据，出了哪一版、发给了谁、对方有没有确认，都要能追溯。
--   只给个下载按钮，事后无法回答"这个供应商当年拿的是哪一版图纸"。
--   故先建发放单（ISS-000001）固定清单（零件的编码/名称/版本快照），
--   再从发放单打包导出；对方回执上传后置 RECEIVED。
--
-- 为什么清单要冗余 code/name/revision：
--   零件后续升版或改名，发放单仍要能还原"当时发出的是什么"，与 sup_part 冗余同理。
--
-- 状态机：DRAFT →send→ SENT →receipt→ RECEIVED；DRAFT 可改可删，SENT 之后只读。
-- 回执文件走 file 中心（不绑 plm_object，只存 file_id + 文件名），
--   因为发放单不是工程对象，不需要版本链；下载仍走 /files/{fileId}/content。
-- =====================================================================

CREATE TABLE sup_issuance (
    id                bigint       PRIMARY KEY,
    company_id        bigint       NOT NULL,
    code              varchar(64)  NOT NULL,                    -- ISS-000001
    supplier_id       bigint       NOT NULL,
    title             varchar(200) NOT NULL,
    type              varchar(16)  NOT NULL DEFAULT 'DESIGN',   -- DESIGN/QUALITY/OTHER
    status            varchar(16)  NOT NULL DEFAULT 'DRAFT',    -- DRAFT/SENT/RECEIVED/CANCELLED
    sent_by           bigint,
    sent_at           timestamptz,
    receipt_file_id   bigint,
    receipt_file_name varchar(255),
    receipt_note      varchar(1000),
    received_by       bigint,
    received_at       timestamptz,
    remark            varchar(1000),
    created_by        bigint,
    created_at        timestamptz  NOT NULL DEFAULT now(),
    updated_by        bigint,
    updated_at        timestamptz  NOT NULL DEFAULT now(),
    deleted           boolean      NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_sup_issuance_code
    ON sup_issuance (company_id, code) WHERE deleted = false;
CREATE INDEX idx_sup_issuance_supplier ON sup_issuance (supplier_id, status)
    WHERE deleted = false;

CREATE TABLE sup_issuance_item (
    id          bigint       PRIMARY KEY,
    company_id  bigint       NOT NULL,
    issuance_id bigint       NOT NULL,
    object_type varchar(64)  NOT NULL DEFAULT 'PdmItem',
    object_id   bigint       NOT NULL,
    item_code   varchar(128),                               -- 快照
    item_name   varchar(200),
    revision    varchar(32),
    sort_no     int          NOT NULL DEFAULT 0
);
CREATE INDEX idx_sup_issuance_item ON sup_issuance_item (issuance_id, sort_no);
CREATE UNIQUE INDEX uk_sup_issuance_item
    ON sup_issuance_item (issuance_id, object_type, object_id);

INSERT INTO sys_permission (code, module, action, name, name_en, perm_type) VALUES
    ('sup.issuance.read',  'SUP', 'READ',  '供应商数据发放查看', 'Supplier issuance read',  'FUNCTION'),
    ('sup.issuance.write', 'SUP', 'WRITE', '供应商数据发放维护', 'Supplier issuance write', 'FUNCTION')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission (role_id, permission_code)
SELECT r.id, p.code
FROM sys_role r
JOIN sys_permission p ON p.code IN ('sup.issuance.read', 'sup.issuance.write')
WHERE r.code IN ('SYS_ADMIN', 'BUSINESS')
ON CONFLICT DO NOTHING;
