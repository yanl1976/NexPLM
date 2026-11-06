-- =====================================================================
-- P6-8 质量规划（简化版）：检验项字典 + 控制计划表
--
-- 三张表：
--   capp_quality_item       检验项主数据（尺寸/外观/性能/材料…的特征与方法）
--   capp_quality_plan       控制计划（按工艺路线，一零件一计划）
--   capp_quality_plan_row   控制计划行 = 工序 + 特性 + 规格公差 + 测量方法 + 抽样 + 控制方法 + 反应计划
--
-- 为什么「行内联特性」而不是强制引用检验项：
--   真实控制计划里大量特性是「一次性」的（某道工序某个尺寸），并不都值得沉淀成主数据；
--   强制引用会让填表变成维护字典。因此 item_id 可空（选了字典就带出 spec/method/gauge 默认值），
--   行上的 characteristic/spec/method/gauge 才是权威值——字典改了不影响已发布计划的历史口径。
--
-- 行的粒度：step_seq_no 绑工序号（与工步一致用语义键，不用 capp_routing_step.id）；
--   substep_seq_no 可选，用于把特性落到具体工步（如「装夹后测量跳动」）。
--
-- 权限：capp.quality.read / capp.quality.write。
-- =====================================================================

CREATE TABLE capp_quality_item (
    id         bigint       PRIMARY KEY,
    company_id bigint       NOT NULL,
    code       varchar(64)  NOT NULL,                     -- QI-000001
    name       varchar(200) NOT NULL,
    category   varchar(32)  NOT NULL DEFAULT 'DIMENSION', -- DIMENSION 尺寸 / APPEARANCE 外观 / PERFORMANCE 性能 / MATERIAL 材料
    spec       varchar(200),                              -- 规格/公差默认值
    method     varchar(200),                              -- 检验方法
    gauge      varchar(200),                              -- 量具/检具
    unit       varchar(32),
    remark     varchar(500),
    created_by bigint,
    created_at timestamptz  NOT NULL DEFAULT now(),
    updated_by bigint,
    updated_at timestamptz  NOT NULL DEFAULT now(),
    deleted    boolean      NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_capp_quality_item_code
    ON capp_quality_item (company_id, code) WHERE deleted = false;
CREATE INDEX idx_capp_quality_item_list
    ON capp_quality_item (company_id, category, updated_at DESC) WHERE deleted = false;

CREATE TABLE capp_quality_plan (
    id         bigint       PRIMARY KEY,
    company_id bigint       NOT NULL,
    code       varchar(64)  NOT NULL,                     -- QP-000001
    name       varchar(200) NOT NULL,
    routing_id bigint       NOT NULL,
    object_id  bigint       NOT NULL,
    status     varchar(16)  NOT NULL DEFAULT 'DRAFT',     -- DRAFT / RELEASED
    remark     varchar(500),
    created_by bigint,
    created_at timestamptz  NOT NULL DEFAULT now(),
    updated_by bigint,
    updated_at timestamptz  NOT NULL DEFAULT now(),
    deleted    boolean      NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_capp_quality_plan_code
    ON capp_quality_plan (company_id, code) WHERE deleted = false;
CREATE INDEX idx_capp_quality_plan_routing
    ON capp_quality_plan (company_id, routing_id) WHERE deleted = false;

CREATE TABLE capp_quality_plan_row (
    id             bigint       PRIMARY KEY,
    company_id     bigint       NOT NULL,
    plan_id        bigint       NOT NULL,
    seq_no         int          NOT NULL,                 -- 行号 10/20/30
    step_seq_no    int,                                   -- 工序号（语义键）
    substep_seq_no int,                                   -- 工步号（可选）
    step_name      varchar(100),                          -- 工序名称快照（工序改名不影响历史计划）
    item_id        bigint,                                -- 检验项字典（可空）
    characteristic varchar(200),                          -- 特性/受控项目
    spec           varchar(200),                          -- 规格/公差
    method         varchar(200),                          -- 评价/测量技术
    gauge          varchar(200),                          -- 量具
    sample_size    varchar(64),                           -- 样本容量
    frequency      varchar(64),                           -- 抽样频率
    control_method varchar(200),                          -- 控制方法
    reaction_plan  varchar(200),                          -- 反应计划
    critical       boolean      NOT NULL DEFAULT false,    -- 关键特性
    remark         varchar(500),
    created_at     timestamptz  NOT NULL DEFAULT now(),
    updated_at     timestamptz  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_capp_quality_row_seq ON capp_quality_plan_row (plan_id, seq_no);
CREATE INDEX idx_capp_quality_row_plan ON capp_quality_plan_row (plan_id, seq_no);

INSERT INTO sys_permission (code, module, action, name, name_en, perm_type) VALUES
    ('capp.quality.read',  'CAPP', 'READ',  '质量规划查看', 'Quality read',  'FUNCTION'),
    ('capp.quality.write', 'CAPP', 'WRITE', '质量规划维护', 'Quality write', 'FUNCTION')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission (role_id, permission_code)
SELECT r.id, p.code
  FROM sys_role r
  JOIN sys_permission p ON p.code LIKE 'capp.quality.%'
 WHERE r.code IN ('SYS_ADMIN', 'BUSINESS')
ON CONFLICT DO NOTHING;
