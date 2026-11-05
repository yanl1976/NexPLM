-- =====================================================================
-- P2 工作空间：文件夹树（我的/公共/组空间）、文件夹内容、收藏、最近访问
-- 说明：path 为物化路径（/{id}/{id}/...），移动文件夹时按前缀批量重写子树；
--       ws_folder 逻辑删除，唯一索引只约束未删除行。
-- =====================================================================

CREATE TABLE ws_folder (
    id          bigint       PRIMARY KEY,
    company_id  bigint       NOT NULL,
    org_id      bigint,
    space_type  varchar(16)  NOT NULL DEFAULT 'MY',
    parent_id   bigint,
    path        varchar(1000) NOT NULL,
    name        varchar(200) NOT NULL,
    owner_id    bigint       NOT NULL,
    group_id    bigint,
    acl_inherit boolean      NOT NULL DEFAULT true,
    sort        int          NOT NULL DEFAULT 0,
    deleted     boolean      NOT NULL DEFAULT false,
    created_by  bigint,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_by  bigint,
    updated_at  timestamptz  NOT NULL DEFAULT now()
);
CREATE INDEX idx_ws_folder_parent ON ws_folder (parent_id, sort);
CREATE INDEX idx_ws_folder_owner ON ws_folder (owner_id, space_type);
CREATE INDEX idx_ws_folder_space ON ws_folder (space_type, company_id);
CREATE UNIQUE INDEX uk_ws_folder_path ON ws_folder (path) WHERE deleted = false;

CREATE TABLE ws_folder_content (
    id         bigint      PRIMARY KEY,
    folder_id  bigint      NOT NULL,
    object_id  bigint      NOT NULL,
    sort       int         NOT NULL DEFAULT 0,
    created_by bigint,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_ws_folder_content ON ws_folder_content (folder_id, object_id);
CREATE INDEX idx_ws_folder_content_object ON ws_folder_content (object_id);

CREATE TABLE ws_favorite (
    id         bigint      PRIMARY KEY,
    user_id    bigint      NOT NULL,
    object_id  bigint      NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_ws_favorite ON ws_favorite (user_id, object_id);

CREATE TABLE ws_recent (
    user_id     bigint      NOT NULL,
    object_id   bigint      NOT NULL,
    accessed_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, object_id)
);
CREATE INDEX idx_ws_recent_user ON ws_recent (user_id, accessed_at DESC);
