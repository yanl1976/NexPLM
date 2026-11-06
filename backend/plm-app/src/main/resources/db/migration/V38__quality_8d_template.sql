-- =====================================================================
-- P8-2 8D 模板可配 + 质量看板
--
-- 模板 = 「一张整改清单」：步骤（D1~D8 或企业自定义）各自带措施类型与提示语。
-- 应用到某个质量问题 = 按模板顺序把步骤批量写成该轮的整改措施（content 预填提示语，
-- 由责任人改成实际内容）——不另建"步骤实例表"：
--   * 措施本来就是"要做的一件事"，步骤只是它的标题，两套表会让"完成度"要算两遍；
--   * 必填校验天然落在既有规则上（提交验证要求当前轮措施全部 DONE）。
-- 模板本身可停用（老问题用过的模板删了也不影响，因为生成的是快照措施）。
--
-- 看板口径（写在服务端注释里，前端只展示）：
--   关闭率 = 已关闭 / 全部（全量快照，不按期间，否则期间内新登记的问题会把关闭率永远压低）；
--   趋势按月（登记数 / 关闭数），期间参数只影响趋势窗口与"本期新增"。
-- =====================================================================

CREATE TABLE qlt_issue_template (
    id          bigint       PRIMARY KEY,
    company_id  bigint       NOT NULL,
    code        varchar(64)  NOT NULL,                      -- DT-000001
    name        varchar(200) NOT NULL,
    template_type varchar(32) NOT NULL DEFAULT '8D',        -- 8D / CUSTOM
    remark      varchar(500),
    enabled     boolean      NOT NULL DEFAULT true,
    created_by  bigint,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_by  bigint,
    updated_at  timestamptz  NOT NULL DEFAULT now(),
    deleted     boolean      NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_qlt_template_code ON qlt_issue_template (company_id, code) WHERE deleted = false;

CREATE TABLE qlt_issue_template_item (
    id          bigint       PRIMARY KEY,
    company_id  bigint       NOT NULL,
    template_id bigint       NOT NULL,
    item_code   varchar(16)  NOT NULL,                      -- D1 / D2 … 或 1/2/3
    name        varchar(200) NOT NULL,
    action_type varchar(16)  NOT NULL DEFAULT 'CORRECTIVE',  -- 复用问题措施类型
    prompt      varchar(2000),                              -- 预填到措施内容的提示语
    required    boolean      NOT NULL DEFAULT true,
    sort_no     int          NOT NULL DEFAULT 10,
    deleted     boolean      NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_qlt_template_item ON qlt_issue_template_item (template_id, item_code)
    WHERE deleted = false;

-- 问题记录本次使用了哪个模板（只做展示与追溯，删除模板不影响已生成措施）
ALTER TABLE qlt_issue ADD COLUMN template_id    bigint;
ALTER TABLE qlt_issue ADD COLUMN template_code  varchar(64);
-- 记录"哪个轮次应用过模板"：同一轮重复点应用会生成重复措施，用它挡住
ALTER TABLE qlt_issue ADD COLUMN template_round int;

INSERT INTO sys_permission (code, module, action, name, name_en, perm_type) VALUES
    ('qlt.template.read',  'QLT', 'READ',  '质量模板查看', 'Quality template read',  'FUNCTION'),
    ('qlt.template.write', 'QLT', 'WRITE', '质量模板维护', 'Quality template write', 'FUNCTION')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission (role_id, permission_code)
SELECT r.id, p.code
FROM sys_role r
JOIN sys_permission p ON p.code LIKE 'qlt.%'
WHERE r.code IN ('SYS_ADMIN', 'BUSINESS')
ON CONFLICT DO NOTHING;
