-- =====================================================================
-- P8-4 供应商主数据与零件关系
--
-- 为什么不建在 plm_object 里：
--   供应商不是工程对象——不需要版本（revision/iteration）、不走生命周期状态机、
--   也不参与 BOM 结构；把供应商塞进通用对象层只会让"供应商升版"这种无意义操作
--   出现在界面上。故独立主表，权限/审计字段与其它业务表保持同一形状。
--
-- 零件关系用 object_type + object_id 指向 plm_object（默认 PdmItem），
-- 并冗余 item_code/item_name：即使零件对象被删/改名，关系表里也还能看出当年挂的是哪个件。
--
-- 多源规则：
--   * 一个零件可以挂多个供应商（多源供货）；
--   * 同一零件的 preferred 只能有一个——由服务层在同一事务里互斥，不靠库约束，
--     因为"互斥"是跨行条件（PostgreSQL 只能用部分唯一索引表达单条供应商维度，
--     这里要表达的是 object 维度，用 EXCLUDE 代价过高）。
-- =====================================================================

CREATE TABLE sup_supplier (
    id            bigint       PRIMARY KEY,
    company_id    bigint       NOT NULL,
    code          varchar(64)  NOT NULL,                    -- SUP-000001
    name          varchar(200) NOT NULL,
    short_name    varchar(100),
    category      varchar(16)  NOT NULL DEFAULT 'MATERIAL',
                                                            -- MATERIAL/OUTSOURCE/SERVICE/LOGISTICS
    status        varchar(16)  NOT NULL DEFAULT 'PENDING',  -- PENDING/QUALIFIED/SUSPENDED/DISQUALIFIED
    contact       varchar(64),
    phone         varchar(64),
    email         varchar(128),
    address       varchar(500),
    tax_no        varchar(64),
    currency      varchar(8)   NOT NULL DEFAULT 'CNY',
    payment_terms varchar(100),                             -- 账期（30 天/月结 60 天…）
    org_id        bigint,                                   -- 归属组织（sys_org）
    attrs         jsonb,                                    -- 可配字段（P8-5 准入评级用）
    remark        varchar(1000),
    created_by    bigint,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    updated_by    bigint,
    updated_at    timestamptz  NOT NULL DEFAULT now(),
    deleted       boolean      NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_sup_supplier_code ON sup_supplier (company_id, code) WHERE deleted = false;
CREATE INDEX idx_sup_supplier_status ON sup_supplier (company_id, status) WHERE deleted = false;

CREATE TABLE sup_part (
    id             bigint        PRIMARY KEY,
    company_id     bigint        NOT NULL,
    supplier_id    bigint        NOT NULL,
    object_type    varchar(64)   NOT NULL DEFAULT 'PdmItem',
    object_id      bigint        NOT NULL,
    item_code      varchar(128),                            -- 冗余展示
    item_name      varchar(200),
    supply_type    varchar(16)   NOT NULL DEFAULT 'BUY',    -- BUY/OUTSOURCE/DOUBLE
    preferred      boolean       NOT NULL DEFAULT false,    -- 同零件唯一首选（服务层互斥）
    lead_time_days int,
    moq            numeric(18,4),                           -- 最小起订量
    price          numeric(18,4),                           -- 参考单价（不含税）
    currency       varchar(8)    NOT NULL DEFAULT 'CNY',
    status         varchar(16)   NOT NULL DEFAULT 'ACTIVE', -- ACTIVE/INACTIVE
    remark         varchar(1000),
    created_by     bigint,
    created_at     timestamptz   NOT NULL DEFAULT now(),
    updated_by     bigint,
    updated_at     timestamptz   NOT NULL DEFAULT now(),
    deleted        boolean       NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_sup_part ON sup_part (company_id, supplier_id, object_type, object_id)
    WHERE deleted = false;
CREATE INDEX idx_sup_part_item ON sup_part (company_id, object_type, object_id) WHERE deleted = false;
CREATE INDEX idx_sup_part_supplier ON sup_part (supplier_id, status) WHERE deleted = false;

INSERT INTO sys_permission (code, module, action, name, name_en, perm_type) VALUES
    ('sup.supplier.read',  'SUP', 'READ',  '供应商查看', 'Supplier read',  'FUNCTION'),
    ('sup.supplier.write', 'SUP', 'WRITE', '供应商维护', 'Supplier write', 'FUNCTION')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission (role_id, permission_code)
SELECT r.id, p.code
FROM sys_role r
JOIN sys_permission p ON p.code IN ('sup.supplier.read', 'sup.supplier.write')
WHERE r.code IN ('SYS_ADMIN', 'BUSINESS')
ON CONFLICT DO NOTHING;
