-- =====================================================================
-- P2 全文检索：文档检索向量表达式索引 + 保存的查询条件
-- 说明：
--   1. 不落冗余列，直接用表达式索引（PostgreSQL 会用同一表达式匹配索引）；
--   2. 检索文本 = 编码 + 名称 + 关键词 + 描述；
--   3. 中文分词依赖数据库配置，simple 配置下中文按整段成为一个词元，
--      因此查询侧同时用 ILIKE 兜底，保证「装配」能命中「主传动装配图」；
--   4. doc_saved_query 保存高级查询条件（JSON），支持个人与共享。
-- =====================================================================

CREATE INDEX idx_plm_object_doc_search ON plm_object USING gin (
    to_tsvector('simple',
        coalesce(code, '') || ' ' || coalesce(name, '') || ' ' ||
        coalesce(ext_json ->> 'keyword', '') || ' ' || coalesce(ext_json ->> 'description', ''))
);

CREATE TABLE doc_saved_query (
    id             bigint       PRIMARY KEY,
    name           varchar(100) NOT NULL,
    entity_code    varchar(64)  NOT NULL DEFAULT 'PdmDoc',
    owner_id       bigint       NOT NULL,
    condition_json jsonb        NOT NULL DEFAULT '{}'::jsonb,
    shared         boolean      NOT NULL DEFAULT false,
    deleted        boolean      NOT NULL DEFAULT false,
    created_by     bigint,
    created_at     timestamptz  NOT NULL DEFAULT now(),
    updated_by     bigint,
    updated_at     timestamptz  NOT NULL DEFAULT now()
);
CREATE INDEX idx_doc_saved_query_owner ON doc_saved_query (owner_id, entity_code);
