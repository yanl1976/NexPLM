-- ---------------------------------------------------------------------
-- BOM 基线（结构冻结快照）：对某根件 + 视图的当前展开结果做不可变快照，
-- 用于后续「新增 / 删除 / 用量变更」对比（旧系统 BASELINEOBJECT / 基线对比）。
--
-- 匹配键采用层级路径 path（由各级「位置」串成，如 10.20），
-- 因为同一子件可在多处出现，path 才是稳定标识（旧系统 LPATH 语义一致）。
-- ---------------------------------------------------------------------
CREATE TABLE bom_baseline (
    id             bigint       PRIMARY KEY,
    root_object_id bigint       NOT NULL,
    code           varchar(64)  NOT NULL,
    name           varchar(200),
    view_type      varchar(16)  NOT NULL DEFAULT 'DESIGN',
    line_count     int          NOT NULL DEFAULT 0,
    frozen_at      timestamptz  NOT NULL DEFAULT now(),
    remark         varchar(500),
    deleted        boolean      NOT NULL DEFAULT false,
    created_by     bigint,
    created_at     timestamptz  NOT NULL DEFAULT now(),
    updated_by     bigint,
    updated_at     timestamptz  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uk_bom_baseline_code ON bom_baseline (root_object_id, code)
    WHERE deleted = false;
CREATE INDEX idx_bom_baseline_root ON bom_baseline (root_object_id, frozen_at DESC);

CREATE TABLE bom_baseline_line (
    id               bigint        PRIMARY KEY,
    baseline_id      bigint        NOT NULL,
    level            int           NOT NULL,
    path             text          NOT NULL,
    parent_object_id bigint,
    child_object_id  bigint        NOT NULL,
    line_no          int           NOT NULL,
    qty              numeric(18,6) NOT NULL,
    loss_rate        numeric(6,3)  NOT NULL DEFAULT 0,
    unit             varchar(32),
    effect_from      date,
    effect_to        date
);

CREATE UNIQUE INDEX uk_bom_baseline_line ON bom_baseline_line (baseline_id, path);
CREATE INDEX idx_bom_baseline_line_child ON bom_baseline_line (baseline_id, child_object_id);
