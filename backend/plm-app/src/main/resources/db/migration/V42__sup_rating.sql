-- =====================================================================
-- P8-5-1 供应商评级（维度可配方案 + 评级快照 + 等级回写）
--
-- 为什么要"方案 + 维度"两张表，而不是把维度写死成质量/交付/价格/服务：
--   IATF16949 下不同类别供应商的考核维度与权重差异很大（外协看过程能力，
--   物流看时效与货损，服务类看响应），写死字段就只能靠改代码适配客户。
--   方案让"评什么、各占多少分"变成数据；同一方案内权重合计必须 100，
--   否则总分口径会随方案漂移（服务层校验，见 SupplierRatingService）。
--
-- 为什么评级要存"维度快照"（sup_rate_item 里冗余 code/name/weight/max_score）：
--   方案改了权重或删了维度，历史评级的分数不该跟着变——审计要的是
--   "当时按什么标准打的分"，和 P8-2 8D 模板生成措施的快照语义一致。
--
-- 等级阈值存 jsonb（grade_rule）而不是四列：档位数与阈值都属业务可配，
--   数组顺序即"从高到低"，服务层据此判定"低于等于 suspend_grade 自动暂停"。
-- =====================================================================

ALTER TABLE sup_supplier ADD COLUMN grade        varchar(8);   -- A/B/C/D（由评级回写）
ALTER TABLE sup_supplier ADD COLUMN rating_score numeric(6,2); -- 最近一次确认的加权总分
ALTER TABLE sup_supplier ADD COLUMN rated_at     timestamptz;

CREATE TABLE sup_rate_scheme (
    id            bigint       PRIMARY KEY,
    company_id    bigint       NOT NULL,
    code          varchar(64)  NOT NULL,                    -- RATE-000001
    name          varchar(200) NOT NULL,
    category      varchar(16),                              -- 适用供应商类别；空 = 通用
    grade_rule    jsonb        NOT NULL,                    -- [{"grade":"A","min":90}, ...]
    suspend_grade varchar(8),                               -- 达到该等级自动暂停供应商
    status        varchar(16)  NOT NULL DEFAULT 'ACTIVE',   -- ACTIVE/INACTIVE
    remark        varchar(1000),
    created_by    bigint,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    updated_by    bigint,
    updated_at    timestamptz  NOT NULL DEFAULT now(),
    deleted       boolean      NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_sup_rate_scheme_code
    ON sup_rate_scheme (company_id, code) WHERE deleted = false;

CREATE TABLE sup_rate_dim (
    id          bigint       PRIMARY KEY,
    company_id  bigint       NOT NULL,
    scheme_id   bigint       NOT NULL,
    code        varchar(32)  NOT NULL,
    name        varchar(64)  NOT NULL,
    weight      numeric(6,2) NOT NULL,                      -- 权重（方案内合计 100）
    max_score   numeric(6,2) NOT NULL DEFAULT 100,
    sort_no     int          NOT NULL DEFAULT 0,
    enabled     boolean      NOT NULL DEFAULT true,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_at  timestamptz  NOT NULL DEFAULT now(),
    deleted     boolean      NOT NULL DEFAULT false
);
CREATE INDEX idx_sup_rate_dim_scheme ON sup_rate_dim (scheme_id) WHERE deleted = false;

CREATE TABLE sup_rate (
    id            bigint       PRIMARY KEY,
    company_id    bigint       NOT NULL,
    supplier_id   bigint       NOT NULL,
    scheme_id     bigint       NOT NULL,
    scheme_code   varchar(64),                              -- 快照
    scheme_name   varchar(200),                             -- 快照
    period        varchar(20)  NOT NULL,                    -- 2026-Q4 / 2026-11
    status        varchar(16)  NOT NULL DEFAULT 'DRAFT',    -- DRAFT/CONFIRMED
    total_score   numeric(6,2),
    grade         varchar(8),
    rated_by      bigint,
    rated_at      timestamptz,
    confirmed_by  bigint,
    confirmed_at  timestamptz,
    remark        varchar(1000),
    created_by    bigint,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    updated_by    bigint,
    updated_at    timestamptz  NOT NULL DEFAULT now(),
    deleted       boolean      NOT NULL DEFAULT false
);
-- 同一供应商 + 同一方案 + 同一周期只留一条评级（重评请删除草稿或另开周期）
CREATE UNIQUE INDEX uk_sup_rate
    ON sup_rate (company_id, supplier_id, scheme_id, period) WHERE deleted = false;
CREATE INDEX idx_sup_rate_supplier ON sup_rate (supplier_id, status) WHERE deleted = false;

CREATE TABLE sup_rate_item (
    id            bigint       PRIMARY KEY,
    company_id    bigint       NOT NULL,
    rating_id     bigint       NOT NULL,
    dim_id        bigint,                                   -- 原维度 id（仅追溯，可为空）
    dim_code      varchar(32)  NOT NULL,
    dim_name      varchar(64)  NOT NULL,
    weight        numeric(6,2) NOT NULL,                    -- 快照
    max_score     numeric(6,2) NOT NULL,                    -- 快照
    score         numeric(6,2) NOT NULL,
    comment       varchar(500),
    sort_no       int          NOT NULL DEFAULT 0
);
CREATE INDEX idx_sup_rate_item ON sup_rate_item (rating_id);

INSERT INTO sys_permission (code, module, action, name, name_en, perm_type) VALUES
    ('sup.rating.read',  'SUP', 'READ',  '供应商评级查看', 'Supplier rating read',  'FUNCTION'),
    ('sup.rating.write', 'SUP', 'WRITE', '供应商评级维护', 'Supplier rating write', 'FUNCTION')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission (role_id, permission_code)
SELECT r.id, p.code
FROM sys_role r
JOIN sys_permission p ON p.code IN ('sup.rating.read', 'sup.rating.write')
WHERE r.code IN ('SYS_ADMIN', 'BUSINESS')
ON CONFLICT DO NOTHING;

-- 通用维度方案示例（可直接用，也可在界面上改）：
--   质量 40 / 交付 30 / 价格 20 / 服务 10，A>=90 B>=75 C>=60 D<60，D 级自动暂停。
INSERT INTO sup_rate_scheme (id, company_id, code, name, category, grade_rule, suspend_grade,
                             status, remark)
SELECT 900000000000000001, c.id, 'RATE-DEFAULT', '通用供应商季度评级', NULL,
       '[{"grade":"A","min":90},{"grade":"B","min":75},{"grade":"C","min":60},{"grade":"D","min":0}]'::jsonb,
       'D', 'ACTIVE', '系统预置：质量/交付/价格/服务四维，可按类别另建方案'
FROM (SELECT id FROM sys_company ORDER BY id LIMIT 1) c
WHERE NOT EXISTS (SELECT 1 FROM sup_rate_scheme WHERE code = 'RATE-DEFAULT');

INSERT INTO sup_rate_dim (id, company_id, scheme_id, code, name, weight, max_score, sort_no)
SELECT 900000000000000100 + v.seq, s.company_id, s.id, v.code, v.name, v.weight, 100, v.seq
FROM sup_rate_scheme s
CROSS JOIN (VALUES
    (1, 'QUALITY', '质量', 40),
    (2, 'DELIVERY', '交付', 30),
    (3, 'PRICE', '价格', 20),
    (4, 'SERVICE', '服务', 10)
) AS v(seq, code, name, weight)
WHERE s.code = 'RATE-DEFAULT' AND s.deleted = false
  AND NOT EXISTS (SELECT 1 FROM sup_rate_dim d WHERE d.scheme_id = s.id);
