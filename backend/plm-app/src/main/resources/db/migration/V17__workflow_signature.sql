-- =====================================================================
-- P4 工作流：电子签名
--
-- 说明：
--   1. 签名在「任务办理」时自动留痕，记录 who / when / what / meaning 四要素
--      （21 CFR Part 11 的电子记录基本要求），不替代流程本身；只增不改；
--   2. 同时记录当时该对象主文件的 sha256，便于事后核验「签名后文件是否被改」；
--   3. verify_result 只在查询时实时计算，不落库，避免状态过期。
-- =====================================================================

CREATE TABLE wf_signature (
    id                  bigint        PRIMARY KEY,
    object_type         varchar(64)   NOT NULL,
    object_id           bigint        NOT NULL,
    revision_label      varchar(32),
    task_id             varchar(64),
    task_key            varchar(128),
    process_instance_id varchar(64),
    signer_id           bigint        NOT NULL,
    signer_name         varchar(128),
    sign_meaning        varchar(64)   NOT NULL DEFAULT 'APPROVE',
    file_id             bigint,
    file_sha256         char(64),
    sign_time           timestamptz   NOT NULL DEFAULT now(),
    remark              varchar(500)
);
CREATE INDEX idx_wf_signature_object ON wf_signature (object_type, object_id, sign_time DESC);
CREATE INDEX idx_wf_signature_instance ON wf_signature (process_instance_id, sign_time);
CREATE INDEX idx_wf_signature_signer ON wf_signature (signer_id, sign_time DESC);
