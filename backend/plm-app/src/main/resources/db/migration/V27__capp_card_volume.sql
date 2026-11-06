-- =====================================================================
-- P6-5 工艺成册（卡片装订）
--
-- 成册 = 按零件把若干张工艺卡片按固定页序装订成一册，供打印/下发/归档。
--   capp_card_volume       册（编码 CV-xxxxxx，绑零件，状态 DRAFT/RELEASED）
--   capp_card_volume_item  册内条目（card_id + seq_no 页序）
--
-- 为什么单独建册而不是给卡片加个「册号」列：
--   同一张卡片可以在不同册里出现（如「机加工分册」与「总装分册」都引用同一张工序卡），
--   一对多关系放条目表才能表达；给卡片挂单值册号会把关系写死。
--
-- 有序性靠 seq_no（10/20/30 步进），唯一键 (volume_id, seq_no) 保证页序不重；
-- 同一张卡在同一册内只能出现一次 → (volume_id, card_id)。
--
-- 权限复用 capp.card.read/write（成册是卡片的组合视图，不单独授权）。
-- =====================================================================

CREATE TABLE capp_card_volume (
    id         bigint       PRIMARY KEY,
    company_id bigint       NOT NULL,
    code       varchar(64)  NOT NULL,                     -- CV-000001
    name       varchar(200) NOT NULL,
    object_id  bigint       NOT NULL,                     -- 册所属零件
    status     varchar(16)  NOT NULL DEFAULT 'DRAFT',     -- DRAFT / RELEASED
    remark     varchar(500),
    created_by bigint,
    created_at timestamptz  NOT NULL DEFAULT now(),
    updated_by bigint,
    updated_at timestamptz  NOT NULL DEFAULT now(),
    deleted    boolean      NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_capp_volume_code ON capp_card_volume (company_id, code) WHERE deleted = false;
CREATE INDEX idx_capp_volume_object ON capp_card_volume (company_id, object_id) WHERE deleted = false;

CREATE TABLE capp_card_volume_item (
    id         bigint      PRIMARY KEY,
    company_id bigint      NOT NULL,
    volume_id  bigint      NOT NULL,
    card_id    bigint      NOT NULL,
    seq_no     int         NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_capp_volume_item_seq ON capp_card_volume_item (volume_id, seq_no);
CREATE UNIQUE INDEX uk_capp_volume_item_card ON capp_card_volume_item (volume_id, card_id);
CREATE INDEX idx_capp_volume_item_volume ON capp_card_volume_item (volume_id, seq_no);
