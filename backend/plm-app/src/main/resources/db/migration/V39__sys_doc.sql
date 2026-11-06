-- =====================================================================
-- P8-3-1 IATF16949 体系文件：模板 + 文件夹 + 版本 + 审批发布
--
-- 为什么不复用 doc_master/plm_object：
--   1. 通用文档层没有"受控版本号（A/B/C 或 1/2/3）+ 生效日期 + 评审周期 + 作废留档"
--      这套受控语义，硬塞要改 plm_object 通用主表（所有对象类型共用），风险外溢；
--   2. 通用文档的升版只有"变更执行 REVISE"一条路径，体系文件日常修订（改一句条款）
--      不应强制走变更单；
--   3. docs/02 原本就规划了独立的体系文件对象（tf_object：template_id/folder_id/version_no）。
--   文件正文先落 sections(jsonb)（章节化维护，便于按模板生成与逐章审批），
--   附件/正文文件后续通过 file_id 挂接文件中心，不需要重做存储。
--
-- 状态机（只有动作接口，保存接口不暴露 status）：
--   DRAFT ─submit─▶ SUBMITTED ─approve─▶ RELEASED ─revise─▶ DRAFT(version_no+1)
--                        └─reject(必填意见)─▶ DRAFT            └─obsolete─▶ OBSOLETE
-- 发布 = 把当前章节快照写进 sys_doc_version（历史版本只增不改）。
-- =====================================================================

-- 文件夹：组织维度（手册/程序文件/作业指导书/记录表单），与"分类"不同
CREATE TABLE sys_doc_folder (
    id         bigint       PRIMARY KEY,
    company_id bigint       NOT NULL,
    parent_id  bigint,
    code       varchar(64)  NOT NULL,
    name       varchar(200) NOT NULL,
    path       varchar(500) NOT NULL DEFAULT '',   -- /QP/WI，前缀查询子树
    sort_no    int          NOT NULL DEFAULT 10,
    created_by bigint,
    created_at timestamptz  NOT NULL DEFAULT now(),
    updated_by bigint,
    updated_at timestamptz  NOT NULL DEFAULT now(),
    deleted    boolean      NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_sys_doc_folder_code ON sys_doc_folder (company_id, code) WHERE deleted = false;
CREATE INDEX idx_sys_doc_folder_parent ON sys_doc_folder (company_id, parent_id) WHERE deleted = false;

-- 模板：规定文件层级、默认文件夹、评审周期与章节骨架
CREATE TABLE sys_doc_template (
    id                    bigint       PRIMARY KEY,
    company_id            bigint       NOT NULL,
    code                  varchar(64)  NOT NULL,                -- TPL-000001
    name                  varchar(200) NOT NULL,
    doc_level             varchar(16)  NOT NULL DEFAULT 'PROCEDURE',
                                                                 -- MANUAL/PROCEDURE/WI/RECORD
    folder_id             bigint,
    default_review_months int          NOT NULL DEFAULT 12,
    sections              jsonb,                                -- [{key,title,hint,required}]
    remark                varchar(500),
    enabled               boolean      NOT NULL DEFAULT true,
    created_by            bigint,
    created_at            timestamptz  NOT NULL DEFAULT now(),
    updated_by            bigint,
    updated_at            timestamptz  NOT NULL DEFAULT now(),
    deleted               boolean      NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_sys_doc_template_code ON sys_doc_template (company_id, code)
    WHERE deleted = false;

-- 体系文件主档：一行 = 一个受控文件（+ 当前版本号）
CREATE TABLE sys_doc (
    id               bigint       PRIMARY KEY,
    company_id       bigint       NOT NULL,
    folder_id        bigint,
    template_id      bigint,
    code             varchar(64)  NOT NULL,                     -- QP-01 / WI-08（企业自定编号）
    name             varchar(200) NOT NULL,
    doc_level        varchar(16)  NOT NULL DEFAULT 'PROCEDURE',
    version_no       int          NOT NULL DEFAULT 1,           -- 当前（或正在修订的）版本号
    status           varchar(16)  NOT NULL DEFAULT 'DRAFT',
                                                                -- DRAFT/SUBMITTED/RELEASED/OBSOLETE
    owner_id         bigint,                                    -- 文件责任人
    review_months    int          NOT NULL DEFAULT 12,
    effective_date   date,
    next_review_date date,
    summary          varchar(1000),
    change_note      varchar(1000),                             -- 本次（待发布）修订说明
    sections         jsonb,                                     -- [{key,title,content,required}]
    file_id          bigint,                                    -- 正文/附件（文件中心，可空）
    submitted_at     timestamptz,
    submitted_by     bigint,
    approved_at      timestamptz,
    approved_by      bigint,
    approve_note     varchar(1000),
    obsolete_at      timestamptz,
    created_by       bigint,
    created_at       timestamptz  NOT NULL DEFAULT now(),
    updated_by       bigint,
    updated_at       timestamptz  NOT NULL DEFAULT now(),
    deleted          boolean      NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX uk_sys_doc_code ON sys_doc (company_id, code) WHERE deleted = false;
CREATE INDEX idx_sys_doc_folder ON sys_doc (company_id, folder_id, status) WHERE deleted = false;
CREATE INDEX idx_sys_doc_review ON sys_doc (company_id, next_review_date) WHERE deleted = false;

-- 版本快照：发布时写入，只增不改
CREATE TABLE sys_doc_version (
    id          bigint       PRIMARY KEY,
    company_id  bigint       NOT NULL,
    doc_id      bigint       NOT NULL,
    version_no  int          NOT NULL,
    sections    jsonb,
    change_note varchar(1000),
    status      varchar(16)  NOT NULL DEFAULT 'RELEASED',
    released_at timestamptz  NOT NULL DEFAULT now(),
    released_by bigint,
    created_by  bigint,
    created_at  timestamptz  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_sys_doc_version ON sys_doc_version (doc_id, version_no);
CREATE INDEX idx_sys_doc_version_doc ON sys_doc_version (doc_id, released_at DESC);

INSERT INTO sys_permission (code, module, action, name, name_en, perm_type) VALUES
    ('qlt.sysdoc.read',  'QLT', 'READ',  '体系文件查看', 'System doc read',  'FUNCTION'),
    ('qlt.sysdoc.write', 'QLT', 'WRITE', '体系文件维护', 'System doc write', 'FUNCTION')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission (role_id, permission_code)
SELECT r.id, p.code
FROM sys_role r
JOIN sys_permission p ON p.code = 'qlt.sysdoc.read' OR p.code = 'qlt.sysdoc.write'
WHERE r.code IN ('SYS_ADMIN', 'BUSINESS')
ON CONFLICT DO NOTHING;
