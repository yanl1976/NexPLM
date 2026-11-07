-- =====================================================================
-- 批次 C-2｜材料定额（公式库）
--
-- 旧系统取证（PLMSOFT/Server/InforCenter/Formula）：
--   FormulaCreatePage.page 的元素结构：type（类型）/ explain（说明）/ Formula（表达式）/
--   para_Grid（参数表）/ Fun_Tree + FunDetail（可用函数树）→
--   即「一个公式 = 表达式 + 参数定义 + 可用函数」，MaterialCalculatePage 拿公式去算材料用量。
--   Formula.cmdsql 还提供 GetResource（从 CAPPRESOURCECLASS 取资源）与 EXECSql（[SQL] 占位），
--   说明旧系统的"公式"既可算数也可取库值；本批只做算数 + 参数，取库值留到报表批次。
-- =====================================================================

CREATE TABLE capp_formula (
    id           bigint       PRIMARY KEY,
    company_id   bigint       NOT NULL,
    code         varchar(64)  NOT NULL,
    name         varchar(200) NOT NULL,
    -- MATERIAL=材料定额 / TIME=工时 / OTHER
    formula_type varchar(16)  NOT NULL DEFAULT 'MATERIAL',
    -- 表达式，如 (gross - net) / net * 100  或  net * qty / (1 - scrap)
    expression   text         NOT NULL,
    -- 参数定义：[{"name":"net","label":"净重","defaultValue":1,"unit":"kg"}]
    params       jsonb        NOT NULL DEFAULT '[]'::jsonb,
    result_unit  varchar(32),
    explain      varchar(500),
    enabled      boolean      NOT NULL DEFAULT true,
    deleted      boolean      NOT NULL DEFAULT false,
    created_by   bigint,
    created_at   timestamptz  NOT NULL DEFAULT now(),
    updated_at   timestamptz  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uk_capp_formula_code ON capp_formula (company_id, code) WHERE deleted = false;

-- 编码规则：公式 FM-000001
INSERT INTO code_rule (id, code, name, object_type, segments_json, reset_policy, status, remark) VALUES
    (910013, 'CappFormula', '材料定额公式', 'CappFormula',
     '[{"type":"TEXT","value":"FM-"},{"type":"SEQ","width":6,"radix":"DECIMAL"}]', 'NEVER', 'ACTIVE', '批次 C-2 新增')
ON CONFLICT DO NOTHING;
