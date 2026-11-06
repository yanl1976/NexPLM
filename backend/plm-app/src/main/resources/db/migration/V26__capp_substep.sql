-- =====================================================================
-- P6-2（补齐）工步子表 + 工艺过程树
--
-- 工步（Substep）= 工序内部的操作细分（装夹 / 走刀 / 测量…），是工序卡的常见落点。
--
-- 关联键的取舍（承接 EXP-20261106-16 的遗留结论）：
--   不绑 capp_routing_step.id —— 该 id 在「序号重建」时会被物理删除并重插（换号必然换 id），
--   绑 id 会让工步在重建后变孤儿。因此用**语义键** (routing_id, step_seq_no) 关联工序：
--   工序号变了才需要考虑迁移，而迁移在 resequence / 整表替换里显式处理。
--
-- 唯一性：同一工序内的工步号不重复 → (routing_id, step_seq_no, seq_no)。
-- 权限：工步是工艺路线的一部分，沿用 capp.routing.read/write，不新增权限点。
-- =====================================================================

CREATE TABLE capp_routing_substep (
    id            bigint        PRIMARY KEY,
    company_id    bigint        NOT NULL,
    routing_id    bigint        NOT NULL,
    step_seq_no   int           NOT NULL,                     -- 所属工序号（语义键，非 step id）
    seq_no        int           NOT NULL,                     -- 工步号：1 / 2 / 3
    name          varchar(100)  NOT NULL,                     -- 工步名称：装夹 / 粗走刀 / 测量
    content       varchar(500),                               -- 工步内容/操作说明
    resource_id   bigint,                                     -- 工步级工艺资源（可覆盖工序级）
    setup_minutes numeric(10,2) NOT NULL DEFAULT 0,
    run_minutes   numeric(10,2) NOT NULL DEFAULT 0,
    remark        varchar(500),
    created_at    timestamptz   NOT NULL DEFAULT now(),
    updated_at    timestamptz   NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_capp_substep_seq
    ON capp_routing_substep (routing_id, step_seq_no, seq_no);
CREATE INDEX idx_capp_substep_routing ON capp_routing_substep (routing_id, step_seq_no);
