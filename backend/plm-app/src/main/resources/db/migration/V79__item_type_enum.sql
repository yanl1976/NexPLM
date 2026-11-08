-- =====================================================================
-- 物料类型枚举补齐（T-CORE-04）
--
-- 背景（docs/15 3.2）：蓝本 PSEITEM.ITEMTYPE 实测 11 类，新系统 ItemType 只有 8 类
--   （产品/部件/零件/标准件/工装/量具/模具/外购件），缺下面 5 类：
--     PSEITEMCISFORM    → CIS      元器件（36,705 行，蓝本第二大类型）
--     PSEITEMFUWU       → SERVICE  服务（187）
--     PSEITEMFEILIAO    → SCRAP    废料（3）
--     PSEITEMWAIXIE     → OUTSOURCE 外协件（278）
--     PSEITEMSGD        → MANUAL   手工单（3,077）
--
-- 说明：ItemService.normalizeItemType 读 meta_enum，补枚举即生效，无需改代码；
--   编码仍按 `PdmItem:<类型>` 路由（T-CORE-01），要单独编号在编码管理页建规则即可。
-- =====================================================================

INSERT INTO meta_enum_item (enum_code, item_code, item_name, sort) VALUES
    ('ItemType', 'CIS',       '元器件',  90),
    ('ItemType', 'SERVICE',   '服务',    100),
    ('ItemType', 'SCRAP',     '废料',    110),
    ('ItemType', 'OUTSOURCE', '外协件',  120),
    ('ItemType', 'MANUAL',    '手工单',  130)
ON CONFLICT DO NOTHING;
