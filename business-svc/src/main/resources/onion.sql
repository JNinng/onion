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
    owner_user_id int8 NULL,                                                       -- 归属用户 ID
    owner_dept_id int8 NULL,                                                       -- 归属部门 ID
    CONSTRAINT sys_basic_pkey PRIMARY KEY (id)
);
COMMENT
ON TABLE public.basic IS '基础字段示例表';

-- Column comments

COMMENT
ON COLUMN public.basic.id IS '主键 ID';
COMMENT
ON COLUMN public.basic.status IS '数据状态，0：禁用、1：启用';
COMMENT
ON COLUMN public.basic.created_by IS '创建人';
COMMENT
ON COLUMN public.basic.created_at IS '创建时间';
COMMENT
ON COLUMN public.basic.updated_by IS '更新人';
COMMENT
ON COLUMN public.basic.updated_at IS '更新时间';
COMMENT
ON COLUMN public.basic.deleted_at IS '删除时间';
COMMENT
ON COLUMN public.basic.tenant_id IS '租户 ID';
COMMENT
ON COLUMN public.basic.owner_user_id IS '归属用户 ID';
COMMENT
ON COLUMN public.basic.owner_dept_id IS '归属部门 ID';


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
    parent_id     int8 NULL,                                                       -- 父级部门 ID
    "name"        varchar(256) NOT NULL,                                           -- 部门名
    owner_user_id int8         NOT NULL,                                           -- 部门管理员用户
    CONSTRAINT sys_dept_pkey PRIMARY KEY (id)
);
COMMENT
ON TABLE public.sys_dept IS '部门表';

-- Column comments

COMMENT
ON COLUMN public.sys_dept.id IS '部门 ID';
COMMENT
ON COLUMN public.sys_dept.status IS '数据状态，0：禁用、1：启用';
COMMENT
ON COLUMN public.sys_dept.created_by IS '创建人';
COMMENT
ON COLUMN public.sys_dept.created_at IS '创建时间';
COMMENT
ON COLUMN public.sys_dept.updated_by IS '更新人';
COMMENT
ON COLUMN public.sys_dept.updated_at IS '更新人';
COMMENT
ON COLUMN public.sys_dept.deleted_at IS '删除时间';
COMMENT
ON COLUMN public.sys_dept.tenant_id IS '租户 ID';
COMMENT
ON COLUMN public.sys_dept.parent_id IS '父级部门 ID';
COMMENT
ON COLUMN public.sys_dept."name" IS '部门名';
COMMENT
ON COLUMN public.sys_dept.owner_user_id IS '部门管理员用户';


-- public.sys_role definition

-- Drop table

-- DROP TABLE public.sys_role;

CREATE TABLE public.sys_role
(
    id            int8         NOT NULL,                                           -- 主键 ID
    status        int2         NOT NULL DEFAULT 1,                                 -- 数据状态，0：禁用、1：启用
    created_by    int8         NOT NULL,                                           -- 创建人
    created_at    timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,                 -- 创建时间
    updated_by    int8         NOT NULL,                                           -- 更新人
    updated_at    timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,                 -- 更新时间
    deleted_at    timestamp(0) NULL     DEFAULT NULL::timestamp without time zone, -- 删除时间
    tenant_id     varchar(16)  NOT NULL,                                           -- 租户 ID
    "name"        varchar(256) NOT NULL,                                           -- 角色名
    code          varchar(64)  NOT NULL,                                           -- 权限编码
    role_type     int2         NOT NULL DEFAULT 2,                                 -- 角色类型：1-系统预置(不可删除，核心权限不可剥离) 2-租户自定义
    data_scope    int2         NOT NULL DEFAULT 1,                                 -- 数据范围规则：1-仅本人 2-本部门 3-本部门及子部门 4-指定人 5-指定部门 6-全租户
    scope_dept_id int8 NULL,                                                       -- 组织可见范围：限定角色可分配的部门层级。NULL=全租户可分配，具体部门ID=仅该部门及子部门可分配
    remark        varchar(512) NULL,                                               -- 备注
    CONSTRAINT sys_role_pkey PRIMARY KEY (id),
    CONSTRAINT sys_role_tenant_code_un UNIQUE (tenant_id, code),
    CONSTRAINT sys_role_tenant_name_un UNIQUE (tenant_id, name)
);
COMMENT
ON TABLE public.sys_role IS '角色表';

-- Column comments

COMMENT
ON COLUMN public.sys_role.id IS '主键 ID';
COMMENT
ON COLUMN public.sys_role.status IS '数据状态，0：禁用、1：启用';
COMMENT
ON COLUMN public.sys_role.created_by IS '创建人';
COMMENT
ON COLUMN public.sys_role.created_at IS '创建时间';
COMMENT
ON COLUMN public.sys_role.updated_by IS '更新人';
COMMENT
ON COLUMN public.sys_role.updated_at IS '更新时间';
COMMENT
ON COLUMN public.sys_role.deleted_at IS '删除时间';
COMMENT
ON COLUMN public.sys_role.tenant_id IS '租户 ID';
COMMENT
ON COLUMN public.sys_role."name" IS '角色名';
COMMENT
ON COLUMN public.sys_role.code IS '权限编码';
COMMENT
ON COLUMN public.sys_role.role_type IS '角色类型：1-系统预置(不可删除，核心权限不可剥离) 2-租户自定义';
COMMENT
ON COLUMN public.sys_role.data_scope IS '数据范围规则：1-仅本人 2-本部门 3-本部门及子部门 4-指定人 5-指定部门 6-全租户';
COMMENT
ON COLUMN public.sys_role.scope_dept_id IS '组织可见范围：限定角色可分配的部门层级。NULL=全租户可分配，具体部门ID=仅该部门及子部门可分配';
COMMENT
ON COLUMN public.sys_role.remark IS '备注';


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
COMMENT
ON TABLE public.sys_tenant IS '租户表';

-- Column comments

COMMENT
ON COLUMN public.sys_tenant.id IS '租户 ID';
COMMENT
ON COLUMN public.sys_tenant.status IS '数据状态，0：禁用、1：启用';
COMMENT
ON COLUMN public.sys_tenant.created_by IS '创建人';
COMMENT
ON COLUMN public.sys_tenant.created_at IS '创建时间';
COMMENT
ON COLUMN public.sys_tenant.updated_by IS '更新人';
COMMENT
ON COLUMN public.sys_tenant.updated_at IS '更新时间';
COMMENT
ON COLUMN public.sys_tenant.deleted_at IS '删除时间';
COMMENT
ON COLUMN public.sys_tenant.tenant_id IS '租户 ID';
COMMENT
ON COLUMN public.sys_tenant.code IS '租户编码';
COMMENT
ON COLUMN public.sys_tenant."name" IS '租户名';


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
    tenant_id     varchar(16) NULL,                                                -- 租户 ID
    owner_user_id int8 NULL,                                                       -- 归属用户 ID
    owner_dept_id int8 NULL,                                                       -- 归属部门 ID
    "name"        varchar(256) NOT NULL,                                           -- 用户名
    nickname      varchar(256) NOT NULL,                                           -- 昵称
    "password"    text         NOT NULL,                                           -- 密码
    remark        varchar NULL,                                                    -- 备注
    CONSTRAINT sys_user_pkey PRIMARY KEY (id)
);
CREATE UNIQUE INDEX sys_user_username_idx ON public.sys_user USING btree (name);
COMMENT
ON TABLE public.sys_user IS '用户表';

-- Column comments

COMMENT
ON COLUMN public.sys_user.id IS '用户 ID';
COMMENT
ON COLUMN public.sys_user.status IS '数据状态，0：禁用、1：启用';
COMMENT
ON COLUMN public.sys_user.created_by IS '创建人';
COMMENT
ON COLUMN public.sys_user.created_at IS '创建时间';
COMMENT
ON COLUMN public.sys_user.updated_by IS '更新人';
COMMENT
ON COLUMN public.sys_user.updated_at IS '更新时间';
COMMENT
ON COLUMN public.sys_user.deleted_at IS '删除时间';
COMMENT
ON COLUMN public.sys_user.tenant_id IS '租户 ID';
COMMENT
ON COLUMN public.sys_user.owner_user_id IS '归属用户 ID';
COMMENT
ON COLUMN public.sys_user.owner_dept_id IS '归属部门 ID';
COMMENT
ON COLUMN public.sys_user."name" IS '用户名';
COMMENT
ON COLUMN public.sys_user.nickname IS '昵称';
COMMENT
ON COLUMN public.sys_user."password" IS '密码';
COMMENT
ON COLUMN public.sys_user.remark IS '备注';


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

COMMENT
ON COLUMN public.user_role_mapping.user_id IS '用户 ID';
COMMENT
ON COLUMN public.user_role_mapping.role_id IS '角色 ID';
COMMENT
ON COLUMN public.user_role_mapping.deleted_at IS '删除时间';
COMMENT
ON COLUMN public.user_role_mapping.updated_by IS '更新人';
COMMENT
ON COLUMN public.user_role_mapping.updated_at IS '更新时间';

-- public.sys_role_id_scope definition

-- Drop table

-- DROP TABLE public.sys_role_id_scope;

CREATE TABLE public.sys_role_id_scope
(
    id         int8         NOT NULL,                                           -- 主键 ID
    status     int2         NOT NULL DEFAULT 1,                                 -- 数据状态，0：禁用、1：启用
    "type"     int2         NOT NULL DEFAULT 1,                                 -- 数据 ID 类型，1：用户ID、2：部门ID
    role_id    int8         NOT NULL,                                           -- 角色 ID
    data_id    int8         NOT NULL,                                           -- 数据 ID
    created_by int8         NOT NULL,                                           -- 创建人
    created_at timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,                 -- 创建时间
    updated_by int8         NOT NULL,                                           -- 更新人
    updated_at timestamp(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,                 -- 更新时间
    deleted_at timestamp(0) NULL     DEFAULT NULL::timestamp without time zone, -- 删除时间
    tenant_id  varchar(16)  NOT NULL,                                           -- 租户 ID
    CONSTRAINT sys_sys_role_id_scope_pkey PRIMARY KEY (id)
);
COMMENT
ON TABLE public.sys_role_id_scope IS '角色数据 ID 范围表';

-- Column comments

COMMENT
ON COLUMN public.sys_role_id_scope.id IS '主键 ID';
COMMENT
ON COLUMN public.sys_role_id_scope.status IS '数据状态，0：禁用、1：启用';
COMMENT
ON COLUMN public.sys_role_id_scope."type" IS '数据 ID 类型，1：用户ID、2：部门ID';
COMMENT
ON COLUMN public.sys_role_id_scope.role_id IS '角色 ID';
COMMENT
ON COLUMN public.sys_role_id_scope.data_id IS '数据 ID';
COMMENT
ON COLUMN public.sys_role_id_scope.created_by IS '创建人';
COMMENT
ON COLUMN public.sys_role_id_scope.created_at IS '创建时间';
COMMENT
ON COLUMN public.sys_role_id_scope.updated_by IS '更新人';
COMMENT
ON COLUMN public.sys_role_id_scope.updated_at IS '更新时间';
COMMENT
ON COLUMN public.sys_role_id_scope.deleted_at IS '删除时间';
COMMENT
ON COLUMN public.sys_role_id_scope.tenant_id IS '租户 ID';


-- public.sys_dept_closure definition

-- Drop table

-- DROP TABLE public.sys_dept_closure;

CREATE TABLE public.sys_dept_closure
(
    id            bigserial NOT NULL,
    ancestor_id   int8      NOT NULL, -- 祖先节点ID
    descendant_id int8      NOT NULL, -- 后代节点ID
    "depth"       int4      NOT NULL, -- 深度/距离（0表示自身，1表示直接子节点，2表示孙子节点...）
    CONSTRAINT sys_dept_closure_pkey PRIMARY KEY (ancestor_id, descendant_id)
);
CREATE INDEX idx_closure_ancestor ON public.sys_dept_closure USING btree (ancestor_id);
CREATE INDEX idx_closure_descendant ON public.sys_dept_closure USING btree (descendant_id);
COMMENT
ON TABLE public.sys_dept_closure IS '部门闭包表';

-- Column comments

COMMENT
ON COLUMN public.sys_dept_closure.ancestor_id IS '祖先节点ID';
COMMENT
ON COLUMN public.sys_dept_closure.descendant_id IS '后代节点ID';
COMMENT
ON COLUMN public.sys_dept_closure."depth" IS '深度/距离（0表示自身，1表示直接子节点，2表示孙子节点...）';

--创建触发器函数，监听 sys_dept 表的增删改
CREATE
OR REPLACE FUNCTION fn_maintain_dept_closure()
    RETURNS TRIGGER AS
$$
DECLARE
v_old_parent_id INT8;
    v_new_parent_id
INT8;
BEGIN
    -- ================= 1. 新增部门 =================
    IF
(TG_OP = 'INSERT') THEN
        -- 插入自身到自身的路径 (深度0)
        INSERT INTO sys_dept_closure (ancestor_id, descendant_id, depth)
        VALUES (NEW.id, NEW.id, 0);

        -- 如果有父节点，复制父节点的所有祖先关系，并指向新节点（深度+1）
        IF
NEW.parent_id IS NOT NULL THEN
            INSERT INTO sys_dept_closure (ancestor_id, descendant_id, depth)
SELECT c.ancestor_id, NEW.id, c.depth + 1
FROM sys_dept_closure c
WHERE c.descendant_id = NEW.parent_id;
END IF;

RETURN NEW;
END IF;

    -- ================= 2. 删除部门 =================
    IF
(TG_OP = 'DELETE') THEN
        -- 删除与该节点及其所有子节点相关的所有闭包记录
        -- (因为子节点失去了这个祖先)
DELETE
FROM sys_dept_closure
WHERE descendant_id IN (SELECT descendant_id
                        FROM sys_dept_closure
                        WHERE ancestor_id = OLD.id);
RETURN OLD;
END IF;

    -- ================= 3. 移动部门 (修改 parent_id) =================
    IF
(TG_OP = 'UPDATE' AND NEW.parent_id IS DISTINCT FROM OLD.parent_id) THEN
        -- 步骤 A: 断开旧关系
        -- 删除 "旧祖先 -> 该节点及其子节点" 的路径
DELETE
FROM sys_dept_closure
WHERE descendant_id IN (SELECT descendant_id FROM sys_dept_closure WHERE ancestor_id = OLD.id) -- 后代
  AND ancestor_id IN
      (SELECT ancestor_id FROM sys_dept_closure WHERE descendant_id = OLD.id AND ancestor_id
    != OLD.id);
-- 旧祖先(排除自身)

-- 步骤 B: 建立新关系
-- 插入 "新祖先 -> 该节点及其子节点" 的路径
IF
NEW.parent_id IS NOT NULL THEN
            INSERT INTO sys_dept_closure (ancestor_id, descendant_id, depth)
SELECT a.ancestor_id, d.descendant_id, a.depth + d.depth + 1
FROM sys_dept_closure a -- 新的祖先路径
         CROSS JOIN sys_dept_closure d -- 当前节点及其子节点的路径
WHERE a.descendant_id = NEW.parent_id -- 新祖先路径的终点是新父节点
  AND d.ancestor_id = NEW.id          -- 子路径的起点是当前节点
  AND a.ancestor_id != d.descendant_id; -- 避免产生环（自己不能是自己的祖先）
END IF;

RETURN NEW;
END IF;

RETURN COALESCE(NEW, OLD);
END;
$$
LANGUAGE plpgsql;

--将触发器绑定到 sys_dept 表
CREATE TRIGGER trg_maintain_dept_closure
    AFTER INSERT OR
UPDATE OF parent_id OR
DELETE
ON sys_dept
    FOR EACH ROW
    EXECUTE FUNCTION fn_maintain_dept_closure();

--初始化闭包表数据
WITH RECURSIVE dept_path AS (
    -- 1. 锚点：提取所有节点自身的路径（深度为0）
    SELECT id AS ancestor_id,
           id AS descendant_id,
           0  AS depth
    FROM sys_dept

    UNION ALL

    -- 2. 递归：向下查找子节点，深度+1
    SELECT dp.ancestor_id,
           d.id         AS descendant_id,
           dp.depth + 1 AS depth
    FROM dept_path dp
             INNER JOIN sys_dept d ON d.parent_id = dp.descendant_id)
-- 3. 插入到闭包表中
INSERT
INTO sys_dept_closure (ancestor_id, descendant_id, depth)
SELECT ancestor_id, descendant_id, depth
FROM dept_path ON CONFLICT (ancestor_id, descendant_id) DO NOTHING; -- 防止重复插入