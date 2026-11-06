-- ---------------------------------------------------------------------
-- BOM 替代件关系（对齐旧系统 ALTERNATESRELATION，旧 Table 名不是 BOM 行上的分组标记）：
--   item_object_id      被替换件（旧 PEID，须是已在某棵 BOM 中出现的子件）
--   alternate_object_id 备选件（旧 CEID）
--   view_type           局部替代时限定的视图（旧 PVIEWID）；全局替代时该列无意义
--   is_global           是否全局（旧 ISGLOBAL）：true 表示所有视图可用
--   is_two_way          是否双向（旧 ISTWOWAY）：双向表示两者可互相替代
--   priority_level      优先级（旧 PRIORITYLEVEL，字符串，旧系统仅做展示/排序）
--   line_no             排序（旧 LSEQUENCE）
-- 替代关系**不参与** BOM 展开与汇总，仅用于展示与「替换」操作。
-- ---------------------------------------------------------------------
CREATE TABLE bom_alternate (
    id                  bigint       PRIMARY KEY,
    item_object_id      bigint       NOT NULL,
    alternate_object_id bigint       NOT NULL,
    view_type           varchar(16)  NOT NULL DEFAULT 'DESIGN',
    is_global           boolean      NOT NULL DEFAULT false,
    is_two_way          boolean      NOT NULL DEFAULT false,
    priority_level      varchar(100),
    line_no             int          NOT NULL DEFAULT 10,
    remark              varchar(500),
    version             int          NOT NULL DEFAULT 0,
    deleted             boolean      NOT NULL DEFAULT false,
    created_by          bigint,
    created_at          timestamptz  NOT NULL DEFAULT now(),
    updated_by          bigint,
    updated_at          timestamptz  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uk_bom_alternate ON bom_alternate (item_object_id, alternate_object_id, view_type)
    WHERE deleted = false;
CREATE INDEX idx_bom_alternate_item ON bom_alternate (item_object_id, line_no);
CREATE INDEX idx_bom_alternate_alternate ON bom_alternate (alternate_object_id);
