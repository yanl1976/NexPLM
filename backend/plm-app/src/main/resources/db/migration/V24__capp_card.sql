-- =====================================================================
-- P6-4 工艺卡片（模板 + 实例 + 导出）
--
-- 两张表：
--   capp_card_template  卡片模板（表头字段 + 明细列 + 表尾），**同码多版本**
--   capp_card           卡片实例（按路线生成工序行快照）
--
-- 版本化口径（DoD「模板修改后历史卡片版式不串」）：
--   模板用 code 标识一族、version_no 标识版本，唯一键是 (company_id, code, version_no)，
--   因此同一模板可以并存「v1 已归档 / v2 已发布 / v3 草稿」多行；发布新版本只把同族
--   其它版本置 ARCHIVED，不覆盖旧行。
--   卡片落地时**双快照**：既记 template_code + template_version，也把 layout_json 整份
--   复制进卡片行。前者用于「这张卡出自哪个模板版本」的追溯，后者保证模板后续改版
--   （哪怕旧版本行被清理）也不会改变历史卡片的版式。
--
-- layout_json 约定（jsonb，结构见 CappCardService#normalizeLayout）：
--   {
--     "title": "机械加工工序卡",
--     "headerFields": [{"label": "零件编码", "field": "objectCode"}, ...],
--     "columns": [{"key": "seqNo", "title": "工序号", "field": "seqNo", "width": 10, "align": "CENTER"}, ...],
--     "footerText": "编制：____ 审核：____ 批准：____"
--   }
--   field 可用的内置变量见 CappCardService 的 CARD_FIELDS / STEP_FIELDS。
--
-- rows_json 约定：按 columns 的 field 取值后的字符串行（数值已格式化），末行为合计行
--   {"__summary__": "true", ...}，导出时加粗。
-- =====================================================================

CREATE TABLE capp_card_template (
    id          bigint       PRIMARY KEY,
    company_id  bigint       NOT NULL,
    code        varchar(64)  NOT NULL,                      -- CT-000001（同码多版本）
    name        varchar(200) NOT NULL,
    card_type   varchar(32)  NOT NULL DEFAULT 'PROCESS',    -- PROCESS 工艺卡 / CRAFT 工序卡 / INSPECT 检验卡
    version_no  int          NOT NULL DEFAULT 1,
    status      varchar(16)  NOT NULL DEFAULT 'DRAFT',      -- DRAFT / RELEASED / ARCHIVED
    title       varchar(200),                               -- 卡片首行标题
    layout_json jsonb        NOT NULL DEFAULT '{}'::jsonb,
    remark      varchar(500),
    released_at timestamptz,
    created_by  bigint,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_by  bigint,
    updated_at  timestamptz  NOT NULL DEFAULT now(),
    deleted     boolean      NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_capp_card_tpl_version
    ON capp_card_template (company_id, code, version_no) WHERE deleted = false;
CREATE INDEX idx_capp_card_tpl_list
    ON capp_card_template (company_id, status, updated_at DESC) WHERE deleted = false;

CREATE TABLE capp_card (
    id               bigint       PRIMARY KEY,
    company_id       bigint       NOT NULL,
    code             varchar(64)  NOT NULL,                 -- CC-000001
    template_id      bigint       NOT NULL,
    template_code    varchar(64)  NOT NULL,
    template_version int          NOT NULL,
    template_name    varchar(200) NOT NULL,
    title            varchar(200),
    layout_json      jsonb        NOT NULL DEFAULT '{}'::jsonb,   -- 版式快照
    routing_id       bigint       NOT NULL,
    routing_code     varchar(64),
    object_id        bigint       NOT NULL,
    object_code      varchar(64),
    field_values     jsonb        NOT NULL DEFAULT '{}'::jsonb,   -- 卡片级字段覆盖（表头区）
    rows_json        jsonb        NOT NULL DEFAULT '[]'::jsonb,   -- 工序行快照
    remark           varchar(500),
    created_by       bigint,
    created_at       timestamptz  NOT NULL DEFAULT now(),
    updated_by       bigint,
    updated_at       timestamptz  NOT NULL DEFAULT now(),
    deleted          boolean      NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_capp_card_code ON capp_card (company_id, code) WHERE deleted = false;
CREATE INDEX idx_capp_card_routing ON capp_card (company_id, routing_id) WHERE deleted = false;
CREATE INDEX idx_capp_card_template ON capp_card (company_id, template_id) WHERE deleted = false;

-- ---------------------------------------------------------------------
-- 权限点
-- ---------------------------------------------------------------------
INSERT INTO sys_permission (code, module, action, name, name_en, perm_type) VALUES
    ('capp.card.read',  'CAPP', 'READ',  '工艺卡片查看', 'Card read',  'FUNCTION'),
    ('capp.card.write', 'CAPP', 'WRITE', '工艺卡片维护', 'Card write', 'FUNCTION')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission (role_id, permission_code)
SELECT r.id, p.code
  FROM sys_role r
  JOIN sys_permission p ON p.code LIKE 'capp.card.%'
 WHERE r.code IN ('SYS_ADMIN', 'BUSINESS')
ON CONFLICT DO NOTHING;
