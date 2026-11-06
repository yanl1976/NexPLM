-- =====================================================================
-- P8-5-2 供应商准入评审（PENDING → QUALIFIED 的唯一入口）
--
-- 为什么准入要有独立评审单，而不是在供应商编辑页直接改状态：
--   "合格供应商"是 IATF16949 里必须留证据的结论（营业执照、体系证书、样品承认、
--   现场审核都要有结果），直接改状态等于没有评审记录。评审单把"谁在什么时候、
--   按哪些检查项评的、结论是什么"固定下来，供应商状态只是评审结果的投影。
--
-- 检查项暂不做模板表：
--   前端预置一套常用清单（营业执照/体系证书/样品承认/现场审核/产能设备/财务信用），
--   允许逐条增删；后端只校验口径（至少一项、通过时不得有 FAIL）。
--   若客户要求"按类别固定检查大纲"，再加 sup_admit_tmpl 即可，不影响本表结构。
--
-- 状态机：DRAFT →submit→ SUBMITTED →approve→ APPROVED / →reject→ REJECTED
--   * 只有 DRAFT 能改检查项与删除；
--   * APPROVED 是唯一把供应商置为 QUALIFIED 的动作（并写 qualified_at）；
--   * 同一供应商同时只允许一张未结束（DRAFT/SUBMITTED）的评审单，避免两份结论打架。
-- =====================================================================

ALTER TABLE sup_supplier ADD COLUMN qualified_at timestamptz;

CREATE TABLE sup_admission (
    id            bigint       PRIMARY KEY,
    company_id    bigint       NOT NULL,
    code          varchar(64)  NOT NULL,                     -- ADM-000001
    supplier_id   bigint       NOT NULL,
    type          varchar(16)  NOT NULL DEFAULT 'INITIAL',   -- INITIAL/ANNUAL/SPECIAL
    status        varchar(16)  NOT NULL DEFAULT 'DRAFT',     -- DRAFT/SUBMITTED/APPROVED/REJECTED
    planned_date  date,
    actual_date   date,                                      -- 通过/驳回时落实际日期
    leader        varchar(64),                               -- 评审组长（自由文本，不绑用户）
    conclusion    varchar(1000),                             -- 评审结论
    reject_reason varchar(500),
    submitted_by  bigint,
    submitted_at  timestamptz,
    approved_by   bigint,
    approved_at   timestamptz,
    remark        varchar(1000),
    created_by    bigint,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    updated_by    bigint,
    updated_at    timestamptz  NOT NULL DEFAULT now(),
    deleted       boolean      NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_sup_admission_code
    ON sup_admission (company_id, code) WHERE deleted = false;
CREATE INDEX idx_sup_admission_supplier ON sup_admission (supplier_id, status)
    WHERE deleted = false;

CREATE TABLE sup_admission_item (
    id           bigint       PRIMARY KEY,
    company_id   bigint       NOT NULL,
    admission_id bigint       NOT NULL,
    seq          int          NOT NULL DEFAULT 0,
    check_item   varchar(500) NOT NULL,
    result       varchar(8)   NOT NULL DEFAULT 'NA',         -- PASS/FAIL/NA
    evidence     varchar(500),
    remark       varchar(500)
);
CREATE INDEX idx_sup_admission_item ON sup_admission_item (admission_id, seq);

INSERT INTO sys_permission (code, module, action, name, name_en, perm_type) VALUES
    ('sup.admission.read',  'SUP', 'READ',  '供应商准入查看', 'Supplier admission read',  'FUNCTION'),
    ('sup.admission.write', 'SUP', 'WRITE', '供应商准入维护', 'Supplier admission write', 'FUNCTION')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission (role_id, permission_code)
SELECT r.id, p.code
FROM sys_role r
JOIN sys_permission p ON p.code IN ('sup.admission.read', 'sup.admission.write')
WHERE r.code IN ('SYS_ADMIN', 'BUSINESS')
ON CONFLICT DO NOTHING;
