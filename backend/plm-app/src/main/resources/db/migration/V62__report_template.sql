-- =====================================================================
-- 复刻批次 D｜报表管理（替代旧系统 Stimulsoft 报表设计器）
--
-- 旧系统：Stimulsoft Reports（.mrt 模板 + 设计器本体，见 11 文档「不复刻」清单）。
-- 本批改为「模板 = 白名单数据集 + 参数定义」：
--   模板只存「用哪个数据集、显示哪些参数、叫什么名字」，SQL 全在服务端代码里
--   （ReportService.DATASETS）。因此接口不接受、也不可能拼出任意 SQL —— 与
--   P10-2 自定义看板同一套安全边界（V52 注释里已论证过为什么数据集不落库）。
--
-- company_id 为 NULL 表示内置模板（所有公司可见、只读），用户自建模板落本公司。
-- =====================================================================

CREATE TABLE rpt_template (
    id          bigint       PRIMARY KEY,
    company_id  bigint,
    code        varchar(64)  NOT NULL,
    name        varchar(200) NOT NULL,
    -- 数据集白名单键：doc.ledger / bom.detail / change.ecr / capp.routing / project.timesheet
    dataset     varchar(64)  NOT NULL,
    category    varchar(32)  NOT NULL DEFAULT 'OTHER',
    -- 参数定义：[{"key":"dateFrom","label":"创建日期起","type":"DATE"}]，类型/选项以数据集字典为准
    params      jsonb        NOT NULL DEFAULT '[]'::jsonb,
    description varchar(500),
    built_in    boolean      NOT NULL DEFAULT false,
    sort_no     int          NOT NULL DEFAULT 100,
    created_by  bigint,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_by  bigint,
    updated_at  timestamptz  NOT NULL DEFAULT now(),
    deleted     boolean      NOT NULL DEFAULT false
);

CREATE UNIQUE INDEX uk_rpt_template_code ON rpt_template (code) WHERE deleted = false;
CREATE INDEX idx_rpt_template_list ON rpt_template (dataset, built_in, sort_no) WHERE deleted = false;

INSERT INTO sys_permission (code, module, action, name, name_en, perm_type) VALUES
    ('ana.report.read',  'ANA', 'READ',  '报表查看与运行', 'Report read',  'FUNCTION'),
    ('ana.report.write', 'ANA', 'WRITE', '报表模板维护',   'Report write', 'FUNCTION')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission (role_id, permission_code)
SELECT r.id, p.code
FROM sys_role r
JOIN sys_permission p ON p.code IN ('ana.report.read', 'ana.report.write')
WHERE r.code IN ('SYS_ADMIN', 'BUSINESS')
ON CONFLICT DO NOTHING;

-- 首批 5 张内置报表（口径与各模块列表接口一致，不另起口径）
INSERT INTO rpt_template (id, company_id, code, name, dataset, category, params, description, built_in, sort_no) VALUES
    (920001, NULL, 'RPT-DOC-LEDGER', '文档台账', 'doc.ledger', 'DOC',
     '[{"key":"keyword","label":"编码/名称","type":"TEXT"},{"key":"docType","label":"文档类型","type":"SELECT"},{"key":"state","label":"生命周期状态","type":"TEXT"},{"key":"dateFrom","label":"创建日期起","type":"DATE"},{"key":"dateTo","label":"创建日期止","type":"DATE"}]',
     '按公司列文档最新版本的编码/名称/类型/版本/状态/发布日/负责人', true, 10),
    (920002, NULL, 'RPT-BOM-DETAIL', 'BOM 明细', 'bom.detail', 'ITEM',
     '[{"key":"keyword","label":"父/子件编码或名称","type":"TEXT"},{"key":"viewType","label":"视图","type":"SELECT"}]',
     '设计/工艺/制造三视图的 BOM 行明细（父件、子件、用量、损耗、位号）', true, 20),
    (920003, NULL, 'RPT-ECR-EXEC', '变更执行台账', 'change.ecr', 'CHANGE',
     '[{"key":"status","label":"状态","type":"SELECT"},{"key":"changeType","label":"变更类型","type":"SELECT"},{"key":"dateFrom","label":"创建日期起","type":"DATE"},{"key":"dateTo","label":"创建日期止","type":"DATE"}]',
     'ECR 申请/评审/执行/关闭全过程的台账视图', true, 30),
    (920004, NULL, 'RPT-CAPP-ROUTING', '工艺路线汇总', 'capp.routing', 'CAPP',
     '[{"key":"keyword","label":"路线/零件编码或名称","type":"TEXT"},{"key":"status","label":"状态","type":"SELECT"},{"key":"typical","label":"典型工艺","type":"SELECT"}]',
     '按零件汇总路线版本、状态、工序数与总工时（与工艺路线列表同源）', true, 40),
    (920005, NULL, 'RPT-PROJ-TIMESHEET', '项目工时汇总', 'project.timesheet', 'PROJECT',
     '[{"key":"projectCode","label":"项目编码","type":"TEXT"},{"key":"dateFrom","label":"工作日期起","type":"DATE"},{"key":"dateTo","label":"工作日期止","type":"DATE"}]',
     '只统计已审批工时（APPROVED），按项目/任务/人汇总', true, 50)
ON CONFLICT DO NOTHING;
