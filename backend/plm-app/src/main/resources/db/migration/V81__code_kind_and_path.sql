-- =====================================================================
-- 编码种类 / 码值 / 分类编码路径（T-CORE-08，EXP-20261108-44）
--
-- 蓝本真实模型（只读取证 + 三张截图）：
--   CODEKIND(17 种类) → CODERULE(58 码段，树) → CODEVALUE(1,170 码值，树)
--   分类通过 CLASSIFYCLASS.CLASSCODEPATH 绑定：`[种类] ; [码值1] ; [码值2] ; [*]`
--   例：100102 抽出式框架断路器 = 元器件编码规则/1001/02/* → 1001+02+00001
--   264 个分类中 258 个有绑定；其余 6 个（非标自制件/产号…）不自动取号。
--
-- 新系统此前只有 `code_rule` 单层段定义 + `doc_class.code_rule_code` 一条规则，
-- 无法表达「码值」与「多段取值」，故补三层：
--   1. code_kind          编码种类（对应蓝本 CODEKIND，rule_code 指向 code_rule）
--   2. code_segment_value 码值（kind + value_path + code_value + name）
--   3. doc_class.code_path 分类绑定的路径（`Kind_xxx/1001/02/*`）
-- =====================================================================

CREATE TABLE IF NOT EXISTS code_kind (
    code       varchar(64)  PRIMARY KEY,
    name       varchar(200) NOT NULL,
    legacy_id  varchar(64),
    rule_code  varchar(64)  NOT NULL,
    sort       int          NOT NULL DEFAULT 0,
    remark     varchar(500),
    created_at timestamptz  NOT NULL DEFAULT now(),
    updated_at timestamptz  NOT NULL DEFAULT now()
);

COMMENT ON TABLE code_kind IS '编码种类（蓝本 CODEKIND）：每个种类对应一条 code_rule，如 元器件/原材料/外购标准件';
COMMENT ON COLUMN code_kind.legacy_id IS '蓝本 CODEKIND.EID，便于追溯';
COMMENT ON COLUMN code_kind.rule_code IS '新系统 code_rule.code，取号时用它渲染';

CREATE TABLE IF NOT EXISTS code_segment_value (
    id          bigint       PRIMARY KEY,
    kind_code   varchar(64)  NOT NULL,
    segment     varchar(64),
    value_path  varchar(200) NOT NULL,
    code_value  varchar(64)  NOT NULL,
    name        varchar(200),
    sort        int          NOT NULL DEFAULT 0,
    deleted     boolean      NOT NULL DEFAULT false,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_at  timestamptz  NOT NULL DEFAULT now()
);

COMMENT ON TABLE code_segment_value IS '码值（蓝本 CODEVALUE）：value_path 形如 1001 或 1001/02，表达码值树';
CREATE UNIQUE INDEX IF NOT EXISTS uk_code_segment_value
    ON code_segment_value (kind_code, value_path) WHERE deleted = false;

ALTER TABLE doc_class ADD COLUMN IF NOT EXISTS code_path varchar(500);

COMMENT ON COLUMN doc_class.code_path IS
    '分类绑定的编码路径（蓝本 CLASSCODEPATH）：`种类规则/码值1/码值2/*`；为空表示不自动取号（人工/图号）';
