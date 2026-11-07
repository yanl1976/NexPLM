-- =====================================================================
-- 批次 C-1｜工厂结构（工位）与消耗定义
--
-- 旧系统取证（PLMSOFT/Server/InforCenter）：
--   PositionManager/Config/Views/DEEPPOSITIONLINKVIEW.view  → 工位对象（POSITIONITEM）树：
--     ECODE/ENAME/POSITIONLEVEL（层级）/LOGISTICSUNIT（物流单位）/CEID/PEID（父级）
--   ConsumptionStation/Config/Views/CSOBJVIEW.view           → 消耗站对象（CSOBJ）：
--     ECODE/ENAME/ITEMEUID+ITEMECODE/ITEMENAME（消耗物料）/NUM1（用量）/ISREFITEM（按物料引用）
--     SUITABILITY（适用性）/PARENTCODE（所属工位）/FITPRODUCT（适配产品）/ALLROUTE（适用全部路线）
--     PRIMARYPLANTSTRUCTURE（主工厂结构）/PRIMARYPRODUCT（主产品）
--
-- 旧对象是 EAV（EID/ECODE/ENAME + 属性表），这里落成关系表；
-- 「主工厂结构/主产品」在旧系统是单选标记，用 is_primary 表达。
-- =====================================================================

CREATE TABLE capp_position (
    id            bigint       PRIMARY KEY,
    company_id    bigint       NOT NULL,
    code          varchar(64)  NOT NULL,
    name          varchar(200) NOT NULL,
    parent_id     bigint,
    -- PLANT=工厂 / WORKSHOP=车间 / LINE=产线 / STATION=工位 / WORKCENTER=工作地
    position_level varchar(16) NOT NULL DEFAULT 'STATION',
    -- 物流单位（旧 LOGISTICSUNIT）：同一物流单元的工位共用搬运资源
    logistics_unit varchar(64),
    capacity      numeric(18, 4),
    shifts        int,
    remark        varchar(500),
    enabled       boolean      NOT NULL DEFAULT true,
    deleted       boolean      NOT NULL DEFAULT false,
    created_by    bigint,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    updated_at    timestamptz  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uk_capp_position_code ON capp_position (company_id, code) WHERE deleted = false;
CREATE INDEX idx_capp_position_parent ON capp_position (company_id, parent_id);

CREATE TABLE capp_consumption (
    id            bigint       PRIMARY KEY,
    company_id    bigint       NOT NULL,
    code          varchar(64)  NOT NULL,
    name          varchar(200),
    -- 所属工位；为空表示通用消耗定义（不对应具体工位）
    position_id   bigint,
    -- 消耗物料：物料按 FNUMBER 归集（同编码多副本），故存 object_id + 冗余编码/名称
    object_id     bigint,
    item_code     varchar(64),
    item_name     varchar(200),
    qty           numeric(18, 4) NOT NULL DEFAULT 1,
    unit          varchar(32),
    -- 是否按物料引用（旧 ISREFITEM：true=引用物料主数据，false=自由填写）
    ref_item      boolean      NOT NULL DEFAULT true,
    suitability   varchar(200),
    fit_product   varchar(200),
    all_route     boolean      NOT NULL DEFAULT false,
    is_primary    boolean      NOT NULL DEFAULT false,
    remark        varchar(500),
    deleted       boolean      NOT NULL DEFAULT false,
    created_by    bigint,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    updated_at    timestamptz  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uk_capp_consumption_code ON capp_consumption (company_id, code) WHERE deleted = false;
CREATE INDEX idx_capp_consumption_position ON capp_consumption (company_id, position_id);

-- 编码规则：工位 POS-000001、消耗定义 CS-000001（走批次 B 的规则引擎取号）
INSERT INTO code_rule (id, code, name, object_type, segments_json, reset_policy, status, remark) VALUES
    (910011, 'CappPosition', '工位', 'CappPosition',
     '[{"type":"TEXT","value":"POS-"},{"type":"SEQ","width":6,"radix":"DECIMAL"}]', 'NEVER', 'ACTIVE', '批次 C-1 新增'),
    (910012, 'CappConsumption', '消耗定义', 'CappConsumption',
     '[{"type":"TEXT","value":"CS-"},{"type":"SEQ","width":6,"radix":"DECIMAL"}]', 'NEVER', 'ACTIVE', '批次 C-1 新增')
ON CONFLICT DO NOTHING;
