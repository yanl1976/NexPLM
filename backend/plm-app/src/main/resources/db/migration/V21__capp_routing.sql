-- =====================================================================
-- P6-1 工艺 CAPP：工艺路线 + 工序（P6 任务 1）
--
-- 关系：一个零件（plm_object, object_type='PdmItem'）可有 N 条工艺路线，
--       同一零件同时只允许一条 RELEASED（发布时自动把其它已发布置 ARCHIVED）。
--       一条工艺路线 1—N 道工序，seq_no 为工序号（工艺惯例 10/20/30 步进 10）。
--
-- 状态机（简单三态，暂不接 meta_lifecycle）：
--   DRAFT（草稿，可改可删）→ RELEASED（已发布，可用于生产）→ ARCHIVED（已归档）
--
-- 工时口径：setup_minutes（准备/装夹工时，按批）+ run_minutes（单件工时）。
--           路线总工时 = Σ(setup + run)，仅作列表概览，详细汇总在 P6-6 工时定额。
-- =====================================================================

CREATE TABLE capp_routing (
    id            bigint       PRIMARY KEY,
    company_id    bigint       NOT NULL,
    code          varchar(64)  NOT NULL,                      -- RT-000001
    name          varchar(200) NOT NULL,
    object_id     bigint       NOT NULL,                      -- 零件（plm_object.id）
    object_type   varchar(64)  NOT NULL DEFAULT 'PdmItem',
    version_no    int          NOT NULL DEFAULT 1,
    status        varchar(16)  NOT NULL DEFAULT 'DRAFT',      -- DRAFT / RELEASED / ARCHIVED
    applicability varchar(200),                               -- 适用性：机型/项目/通用说明
    remark        varchar(500),
    released_at   timestamptz,
    created_by    bigint,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    updated_by    bigint,
    updated_at    timestamptz  NOT NULL DEFAULT now(),
    deleted       boolean      NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_capp_routing_code ON capp_routing (company_id, code) WHERE deleted = false;
CREATE INDEX idx_capp_routing_object ON capp_routing (company_id, object_id, status) WHERE deleted = false;
CREATE INDEX idx_capp_routing_list ON capp_routing (company_id, status, updated_at DESC) WHERE deleted = false;

CREATE TABLE capp_routing_step (
    id            bigint        PRIMARY KEY,
    company_id    bigint        NOT NULL,
    routing_id    bigint        NOT NULL,
    seq_no        int           NOT NULL,                     -- 工序号：10 / 20 / 30
    name          varchar(100)  NOT NULL,                     -- 工序名称：下料 / 车削 / 检验
    op_type       varchar(32)   NOT NULL DEFAULT 'MACHINING',
                                                              -- MACHINING 机加 / ASSEMBLY 装配 / INSPECTION 检验
                                                              -- HEAT 热处理 / SURFACE 表处 / OTHER 其它
    work_center   varchar(100),                               -- 工作中心/车间（资源库细化在 P6-3）
    setup_minutes numeric(10,2) NOT NULL DEFAULT 0,
    run_minutes   numeric(10,2) NOT NULL DEFAULT 0,
    remark        varchar(500),
    created_at    timestamptz   NOT NULL DEFAULT now(),
    updated_at    timestamptz   NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_capp_step_seq ON capp_routing_step (routing_id, seq_no);
CREATE INDEX idx_capp_step_routing ON capp_routing_step (routing_id, seq_no);

-- ---------------------------------------------------------------------
-- 权限点
-- ---------------------------------------------------------------------
INSERT INTO sys_permission (code, module, action, name, name_en, perm_type) VALUES
    ('capp.routing.read',  'CAPP', 'READ',  '工艺路线查看', 'Routing read',  'FUNCTION'),
    ('capp.routing.write', 'CAPP', 'WRITE', '工艺路线维护', 'Routing write', 'FUNCTION')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission (role_id, permission_code)
SELECT r.id, p.code
  FROM sys_role r
  JOIN sys_permission p ON p.code LIKE 'capp.%'
 WHERE r.code IN ('SYS_ADMIN', 'BUSINESS')
ON CONFLICT DO NOTHING;
