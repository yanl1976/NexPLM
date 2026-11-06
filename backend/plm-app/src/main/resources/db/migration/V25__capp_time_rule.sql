-- =====================================================================
-- P6-6 工时定额（参数化规则）
--
-- 一张规则表 + 只读试算接口，**不落计算结果**：
--   定额 = 规则 × 工序实际工时，每次查询实时算。因此「公式（系数/基准）改动可重算」，
--   也不会出现「路线改了但定额还是旧值」的漂移。
--
-- 计算口径（工艺定额通行做法，见 CappTimeService#calc）：
--   工序准备工时(按批) = setup_base_minutes + 工序 setup_minutes × setup_factor
--   工序单件工时       = 工序 run_minutes × run_factor
--   单件工时合计       = Σ 工序单件工时                 （不含准备工时）
--   批量工时           = Σ 工序准备工时 + 单件工时合计 × batch_qty
--
-- 规则匹配（越靠前越优先，同级按 priority 升序）：
--   ① work_center 精确 + op_type 精确
--   ② work_center 精确 + op_type 通配
--   ③ work_center 通配 + op_type 精确
--   ④ 两者都通配（全局默认规则）
--   无匹配则按系数 1 / 基准 0 计算，并在结果的 unmatchedSteps 里标注。
-- =====================================================================

CREATE TABLE capp_time_rule (
    id                 bigint        PRIMARY KEY,
    company_id         bigint        NOT NULL,
    code               varchar(64)   NOT NULL,                 -- TR-000001
    name               varchar(200)  NOT NULL,
    op_type            varchar(32),                            -- NULL = 适用所有工序类型
    work_center        varchar(100),                           -- NULL = 适用所有工作中心
    setup_base_minutes numeric(10,2) NOT NULL DEFAULT 0,       -- 准备工时基准（每批固定附加）
    setup_factor       numeric(10,4) NOT NULL DEFAULT 1,       -- 准备工时系数
    run_factor         numeric(10,4) NOT NULL DEFAULT 1,       -- 单件工时系数
    batch_qty          int           NOT NULL DEFAULT 1,       -- 建议批量（试算时的默认值）
    priority           int           NOT NULL DEFAULT 100,     -- 同档位内的排序，越小越优先
    enabled            boolean       NOT NULL DEFAULT true,
    remark             varchar(500),
    created_by         bigint,
    created_at         timestamptz   NOT NULL DEFAULT now(),
    updated_by         bigint,
    updated_at         timestamptz   NOT NULL DEFAULT now(),
    deleted            boolean       NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_capp_time_rule_code ON capp_time_rule (company_id, code) WHERE deleted = false;
CREATE INDEX idx_capp_time_rule_match
    ON capp_time_rule (company_id, enabled, op_type, work_center) WHERE deleted = false;

-- ---------------------------------------------------------------------
-- 权限点
-- ---------------------------------------------------------------------
INSERT INTO sys_permission (code, module, action, name, name_en, perm_type) VALUES
    ('capp.time.read',  'CAPP', 'READ',  '工时定额查看', 'Time rule read',  'FUNCTION'),
    ('capp.time.write', 'CAPP', 'WRITE', '工时定额维护', 'Time rule write', 'FUNCTION')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission (role_id, permission_code)
SELECT r.id, p.code
  FROM sys_role r
  JOIN sys_permission p ON p.code LIKE 'capp.time.%'
 WHERE r.code IN ('SYS_ADMIN', 'BUSINESS')
ON CONFLICT DO NOTHING;
