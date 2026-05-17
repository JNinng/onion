-- public.basic definition

-- Drop table

-- DROP TABLE public.basic;

CREATE TABLE public.basic
(
    id            int8         NOT NULL,                                           -- 主键 ID
    status        int2         NOT NULL DEFAULT 1,                                 -- 数据状态，0：禁用、1：启用
    created_by    int8         NOT NULL,                                           -- 创建人
    created_at    timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,                 -- 创建时间
    updated_by    int8         NOT NULL,                                           -- 更新人
    updated_at    timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,                 -- 更新时间
    deleted_at    timestamp(0) NULL     DEFAULT NULL::timestamp without time zone, -- 删除时间
    tenant_id     varchar(16)  NOT NULL,                                           -- 租户 ID
    owner_user_id int8         NULL,                                               -- 归属用户 ID
    owner_dept_id int8         NULL,                                               -- 归属部门 ID
    CONSTRAINT sys_basic_pkey PRIMARY KEY (id)
);
COMMENT ON TABLE public.basic IS '基础字段示例表';

-- Column comments

COMMENT ON COLUMN public.basic.id IS '主键 ID';
COMMENT ON COLUMN public.basic.status IS '数据状态，0：禁用、1：启用';
COMMENT ON COLUMN public.basic.created_by IS '创建人';
COMMENT ON COLUMN public.basic.created_at IS '创建时间';
COMMENT ON COLUMN public.basic.updated_by IS '更新人';
COMMENT ON COLUMN public.basic.updated_at IS '更新时间';
COMMENT ON COLUMN public.basic.deleted_at IS '删除时间';
COMMENT ON COLUMN public.basic.tenant_id IS '租户 ID';
COMMENT ON COLUMN public.basic.owner_user_id IS '归属用户 ID';
COMMENT ON COLUMN public.basic.owner_dept_id IS '归属部门 ID';


-- public.sys_dept definition

-- Drop table

-- DROP TABLE public.sys_dept;

CREATE TABLE public.sys_dept
(
    id            int8         NOT NULL,                                           -- 部门 ID
    status        int2         NOT NULL DEFAULT 1,                                 -- 数据状态，0：禁用、1：启用
    created_by    int8         NOT NULL,                                           -- 创建人
    created_at    timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,                 -- 创建时间
    updated_by    int8         NOT NULL,                                           -- 更新人
    updated_at    timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,                 -- 更新人
    deleted_at    timestamp(0) NULL     DEFAULT NULL::timestamp without time zone, -- 删除时间
    tenant_id     varchar(16)  NOT NULL,                                           -- 租户 ID
    parent_id     int8         NULL,                                               -- 父级部门 ID
    "name"        varchar(256) NOT NULL,                                           -- 部门名
    owner_user_id int8         NOT NULL,                                           -- 部门管理员用户
    CONSTRAINT sys_dept_pkey PRIMARY KEY (id)
);
COMMENT ON TABLE public.sys_dept IS '部门表';

-- Column comments

COMMENT ON COLUMN public.sys_dept.id IS '部门 ID';
COMMENT ON COLUMN public.sys_dept.status IS '数据状态，0：禁用、1：启用';
COMMENT ON COLUMN public.sys_dept.created_by IS '创建人';
COMMENT ON COLUMN public.sys_dept.created_at IS '创建时间';
COMMENT ON COLUMN public.sys_dept.updated_by IS '更新人';
COMMENT ON COLUMN public.sys_dept.updated_at IS '更新人';
COMMENT ON COLUMN public.sys_dept.deleted_at IS '删除时间';
COMMENT ON COLUMN public.sys_dept.tenant_id IS '租户 ID';
COMMENT ON COLUMN public.sys_dept.parent_id IS '父级部门 ID';
COMMENT ON COLUMN public.sys_dept."name" IS '部门名';
COMMENT ON COLUMN public.sys_dept.owner_user_id IS '部门管理员用户';


-- public.sys_role definition

-- Drop table

-- DROP TABLE public.sys_role;

CREATE TABLE public.sys_role
(
    id         int8         NOT NULL,                                           -- 主键 ID
    status     int2         NOT NULL DEFAULT 1,                                 -- 数据状态，0：禁用、1：启用
    created_by int8         NOT NULL,                                           -- 创建人
    created_at timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,                 -- 创建时间
    updated_by int8         NOT NULL,                                           -- 更新人
    updated_at timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,                 -- 更新时间
    deleted_at timestamp(0) NULL     DEFAULT NULL::timestamp without time zone, -- 删除时间
    tenant_id  varchar(16)  NOT NULL,                                           -- 租户 ID
    parent_id  int8         NULL,                                               -- 父角色 ID
    "name"     varchar(256) NOT NULL,                                           -- 角色名
    "scope"    int2         NOT NULL,                                           -- 数据范围规则：0-个人 1-本部门 2-部门及子部门 3-指定人 4-全租户 5-自定义
    code       varchar(64)  NOT NULL,                                           -- 权限编码
    CONSTRAINT sys_role_pkey PRIMARY KEY (id),
    CONSTRAINT sys_role_tenant_code_un UNIQUE (tenant_id, code)
);
COMMENT ON TABLE public.sys_role IS '角色表';

-- Column comments

COMMENT ON COLUMN public.sys_role.id IS '主键 ID';
COMMENT ON COLUMN public.sys_role.status IS '数据状态，0：禁用、1：启用';
COMMENT ON COLUMN public.sys_role.created_by IS '创建人';
COMMENT ON COLUMN public.sys_role.created_at IS '创建时间';
COMMENT ON COLUMN public.sys_role.updated_by IS '更新人';
COMMENT ON COLUMN public.sys_role.updated_at IS '更新时间';
COMMENT ON COLUMN public.sys_role.deleted_at IS '删除时间';
COMMENT ON COLUMN public.sys_role.tenant_id IS '租户 ID';
COMMENT ON COLUMN public.sys_role.parent_id IS '父角色 ID';
COMMENT ON COLUMN public.sys_role."name" IS '角色名';
COMMENT ON COLUMN public.sys_role."scope" IS '数据范围规则：0-个人 1-本部门 2-部门及子部门 3-指定人 4-全租户 5-自定义';
COMMENT ON COLUMN public.sys_role.code IS '权限编码';


-- public.sys_tenant definition

-- Drop table

-- DROP TABLE public.sys_tenant;

CREATE TABLE public.sys_tenant
(
    id         varchar(16)  NOT NULL,                                           -- 租户 ID
    status     int2         NOT NULL DEFAULT 1,                                 -- 数据状态，0：禁用、1：启用
    created_by int8         NOT NULL,                                           -- 创建人
    created_at timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,                 -- 创建时间
    updated_by int8         NOT NULL,                                           -- 更新人
    updated_at timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,                 -- 更新时间
    deleted_at timestamp(0) NULL     DEFAULT NULL::timestamp without time zone, -- 删除时间
    tenant_id  varchar(16)  NOT NULL,                                           -- 租户 ID
    code       varchar(32)  NOT NULL,                                           -- 租户编码
    "name"     varchar(256) NOT NULL,                                           -- 租户名
    CONSTRAINT sys_tenant_name_un UNIQUE (name),
    CONSTRAINT sys_tenant_pk PRIMARY KEY (id),
    CONSTRAINT sys_tenant_un UNIQUE (code)
);
COMMENT ON TABLE public.sys_tenant IS '租户表';

-- Column comments

COMMENT ON COLUMN public.sys_tenant.id IS '租户 ID';
COMMENT ON COLUMN public.sys_tenant.status IS '数据状态，0：禁用、1：启用';
COMMENT ON COLUMN public.sys_tenant.created_by IS '创建人';
COMMENT ON COLUMN public.sys_tenant.created_at IS '创建时间';
COMMENT ON COLUMN public.sys_tenant.updated_by IS '更新人';
COMMENT ON COLUMN public.sys_tenant.updated_at IS '更新时间';
COMMENT ON COLUMN public.sys_tenant.deleted_at IS '删除时间';
COMMENT ON COLUMN public.sys_tenant.tenant_id IS '租户 ID';
COMMENT ON COLUMN public.sys_tenant.code IS '租户编码';
COMMENT ON COLUMN public.sys_tenant."name" IS '租户名';


-- public.sys_user definition

-- Drop table

-- DROP TABLE public.sys_user;

CREATE TABLE public.sys_user
(
    id            int8         NOT NULL,                                           -- 用户 ID
    status        int2         NOT NULL DEFAULT 1,                                 -- 数据状态，0：禁用、1：启用
    created_by    int8         NOT NULL,                                           -- 创建人
    created_at    timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,                 -- 创建时间
    updated_by    int8         NOT NULL,                                           -- 更新人
    updated_at    timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,                 -- 更新时间
    deleted_at    timestamp(0) NULL     DEFAULT NULL::timestamp without time zone, -- 删除时间
    tenant_id     varchar(16)  NULL,                                               -- 租户 ID
    owner_user_id int8         NULL,                                               -- 归属用户 ID
    owner_dept_id int8         NULL,                                               -- 归属部门 ID
    "name"        varchar(256) NOT NULL,                                           -- 用户名
    nickname      varchar(256) NOT NULL,                                           -- 昵称
    "password"    text         NOT NULL,                                           -- 密码
    CONSTRAINT sys_user_pkey PRIMARY KEY (id)
);
CREATE UNIQUE INDEX sys_user_username_idx ON public.sys_user USING btree (name);
COMMENT ON TABLE public.sys_user IS '用户表';

-- Column comments

COMMENT ON COLUMN public.sys_user.id IS '用户 ID';
COMMENT ON COLUMN public.sys_user.status IS '数据状态，0：禁用、1：启用';
COMMENT ON COLUMN public.sys_user.created_by IS '创建人';
COMMENT ON COLUMN public.sys_user.created_at IS '创建时间';
COMMENT ON COLUMN public.sys_user.updated_by IS '更新人';
COMMENT ON COLUMN public.sys_user.updated_at IS '更新时间';
COMMENT ON COLUMN public.sys_user.deleted_at IS '删除时间';
COMMENT ON COLUMN public.sys_user.tenant_id IS '租户 ID';
COMMENT ON COLUMN public.sys_user.owner_user_id IS '归属用户 ID';
COMMENT ON COLUMN public.sys_user.owner_dept_id IS '归属部门 ID';
COMMENT ON COLUMN public.sys_user."name" IS '用户名';
COMMENT ON COLUMN public.sys_user.nickname IS '昵称';
COMMENT ON COLUMN public.sys_user."password" IS '密码';


-- public.user_role_mapping definition

-- Drop table

-- DROP TABLE public.user_role_mapping;

CREATE TABLE public.user_role_mapping
(
    user_id    int8         NOT NULL, -- 用户 ID
    role_id    int8         NOT NULL, -- 角色 ID
    deleted_at timestamp(0) NULL,     -- 删除时间
    updated_by int8         NOT NULL, -- 更新人
    updated_at timestamp(0) NOT NULL  -- 更新时间
);

-- Column comments

COMMENT ON COLUMN public.user_role_mapping.user_id IS '用户 ID';
COMMENT ON COLUMN public.user_role_mapping.role_id IS '角色 ID';
COMMENT ON COLUMN public.user_role_mapping.deleted_at IS '删除时间';
COMMENT ON COLUMN public.user_role_mapping.updated_by IS '更新人';
COMMENT ON COLUMN public.user_role_mapping.updated_at IS '更新时间';