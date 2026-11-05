-- =====================================================================
-- P2 文档预览：预览产物登记与状态
-- 说明：
--   1. 预览产物本身仍存文件仓（file_role=VIEW 的 plm_file + 链接），本表只登记
--      「哪个源文件 → 哪种格式 → 状态 / 失败原因」，便于重试与前端展示；
--   2. status：PENDING 处理中、READY 就绪、FAILED 失败可重试、SKIPPED 环境不支持；
--   3. 同一文档同一源文件只保留一条记录（重新生成时覆盖）。
-- =====================================================================

CREATE TABLE doc_preview (
    id              bigint      PRIMARY KEY,
    object_id       bigint      NOT NULL,
    source_file_id  bigint      NOT NULL,
    preview_file_id bigint,
    format          varchar(16) NOT NULL DEFAULT 'PDF',
    status          varchar(16) NOT NULL DEFAULT 'PENDING',
    message         varchar(500),
    page_count      integer,
    created_by      bigint,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_by      bigint,
    updated_at      timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uk_doc_preview_object_file UNIQUE (object_id, source_file_id)
);
CREATE INDEX idx_doc_preview_object ON doc_preview (object_id);
