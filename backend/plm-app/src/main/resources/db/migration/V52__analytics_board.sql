-- =====================================================================
-- P10-2 自定义看板
--
-- 为什么数据集不落库：
--   "自定义看板"最容易做成"用户填 SQL 的数据集"，那等于把数据库只读权限开放给
--   任意能编辑看板的人（越权读别的公司数据、慢查询拖垮库）。这里的数据集是
--   服务端代码里注册的白名单（见 BoardService.DATASETS），库里只存"用了哪个数据集 +
--   展示方式 + 布局"，因此不会出现任意 SQL。
--
-- 为什么布局存 jsonb 而不是单独组件表：
--   看板是一次读、整体改的编辑对象（拖拽排序后整块保存），拆表反而要处理排序冲突；
--   组件不是独立业务实体，没有按组件查询/统计的需求。
-- =====================================================================

CREATE TABLE ana_board (
    id          bigint       PRIMARY KEY,
    company_id  bigint       NOT NULL,
    code        varchar(64)  NOT NULL,
    name        varchar(200) NOT NULL,
    description varchar(1000),
    -- 组件数组：[{id,type,title,dataset,top,span,order}]
    layout      jsonb        NOT NULL DEFAULT '[]'::jsonb,
    shared      boolean      NOT NULL DEFAULT false,
    created_by  bigint,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_by  bigint,
    updated_at  timestamptz  NOT NULL DEFAULT now(),
    deleted     boolean      NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_ana_board_code ON ana_board (company_id, code) WHERE deleted = false;
CREATE INDEX idx_ana_board_list ON ana_board (company_id, shared, updated_at DESC) WHERE deleted = false;

INSERT INTO sys_permission (code, module, action, name, name_en, perm_type) VALUES
    ('ana.board.read',  'ANA', 'READ',  '自定义看板查看', 'Analytics board read',  'FUNCTION'),
    ('ana.board.write', 'ANA', 'WRITE', '自定义看板维护', 'Analytics board write', 'FUNCTION')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission (role_id, permission_code)
SELECT r.id, p.code
FROM sys_role r
JOIN sys_permission p ON p.code IN ('ana.board.read', 'ana.board.write')
WHERE r.code IN ('SYS_ADMIN', 'BUSINESS')
ON CONFLICT DO NOTHING;
