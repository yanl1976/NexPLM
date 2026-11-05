-- =====================================================================
-- P2 文件仓：分片直传（断点续传）
-- 说明：分片本体落在存储层（key = _multipart/{sessionId}/{index}），
--       库内只记录会话与已收分片的索引，合并成功后清理分片并置 MERGED。
-- =====================================================================

CREATE TABLE file_upload_session (
    id              varchar(64)  PRIMARY KEY,
    file_name       varchar(255) NOT NULL,
    total_size      bigint       NOT NULL,
    chunk_size      int          NOT NULL,
    chunk_total     int          NOT NULL,
    mime            varchar(128),
    owner_object_id bigint,
    file_role       varchar(16)  NOT NULL DEFAULT 'ATTACHMENT',
    sort            int          NOT NULL DEFAULT 0,
    status          varchar(16)  NOT NULL DEFAULT 'UPLOADING',
    created_by      bigint,
    created_at      timestamptz  NOT NULL DEFAULT now(),
    updated_at      timestamptz  NOT NULL DEFAULT now()
);
CREATE INDEX idx_file_upload_session_owner ON file_upload_session (created_by, created_at DESC);
CREATE INDEX idx_file_upload_session_status ON file_upload_session (status, updated_at);

-- 已收到的分片（主键天然幂等：同一分片重复上传只覆盖）
CREATE TABLE file_upload_chunk (
    session_id  varchar(64) NOT NULL,
    chunk_index int         NOT NULL,
    size        int         NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (session_id, chunk_index)
);
