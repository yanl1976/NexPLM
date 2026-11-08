-- =====================================================================
-- 编码码段（T-CORE-15，EXP-20261108-54）
--
-- 蓝本模型是四层：
--   CODEKIND(17 种类) → CODERULE(58 码段，树) → CODEVALUE(1,170 码值，树)
--   → 分类通过 CLASSIFYCLASS.CLASSCODEPATH 绑定
--
-- V81 只落了「种类 + 码值 + 分类路径」，缺中间的**码段**层：
--   码段决定「有几段、每段多长、谁是谁的父段」，是码值树的分层依据
--   （蓝本实测：大类 ord=1/len=0、小类 ord=2/len=0（父=大类）、流水码 ord=3/len=5）。
--
-- 本表只存「段定义」，码值仍由 `code_segment_value` 承载；
-- 数据由 `deploy/migration/071-code_segment.sql` 从蓝本 CODERULE 导入。
-- =====================================================================

CREATE TABLE IF NOT EXISTS code_segment (
    id          bigint       PRIMARY KEY,
    rule_code   varchar(64)  NOT NULL,
    code        varchar(64)  NOT NULL,
    name        varchar(200) NOT NULL,
    order_no    int          NOT NULL DEFAULT 0,
    length      int,
    parent_code varchar(64),
    drop_mode   varchar(32),
    status      varchar(32)  NOT NULL DEFAULT 'ACTIVE',
    remark      varchar(500),
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_at  timestamptz  NOT NULL DEFAULT now()
);

COMMENT ON TABLE code_segment IS
    '编码码段（蓝本 CODERULE）：每个种类一组，parent_code 表达码段树（大类→小类）';
COMMENT ON COLUMN code_segment.rule_code IS '对应 code_rule.code（= code_kind.rule_code）';
COMMENT ON COLUMN code_segment.length IS '段长；0 或空表示不定长（枚举型码值）';
COMMENT ON COLUMN code_segment.parent_code IS '父段 code（同一 rule_code 内），表达码段树';

CREATE UNIQUE INDEX IF NOT EXISTS uk_code_segment ON code_segment (rule_code, code);
CREATE INDEX IF NOT EXISTS idx_code_segment_rule ON code_segment (rule_code, order_no);
