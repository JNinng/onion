package org.ninng.businesssvc.identity.infrastructure;

import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTypedTable;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jspecify.annotations.NonNull;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.identity.application.dto.RoleSpecification;
import org.ninng.businesssvc.identity.application.dto.RoleUpdateInput;
import org.ninng.businesssvc.identity.domain.model.SysRole;
import org.ninng.businesssvc.identity.domain.model.SysRoleProps;
import org.ninng.businesssvc.identity.domain.model.SysRoleTable;
import org.ninng.businesssvc.identity.domain.port.RolePort;
import org.ninng.businesssvc.repository.CommonRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 角色端口接口的 Jimmer 持久化实现。
 *
 * <p>继承 {@link CommonRepository} 复用通用 CRUD 和分页查询能力。
 * 角色实体通过 {@code role_id_scopes} 关联 {@code SysRoleIdScope} 表，
 * 更新时使用 {@code APPEND} 模式追加关联数据。</p>
 *
 * @author onion
 */
@Repository
public class RolePortImpl extends CommonRepository<SysRole, Long> implements RolePort {

    /**
     * Jimmer 编译期生成的角色表定义
     */
    private static final SysRoleTable table = SysRoleTable.$;

    /**
     * 通过 Jimmer SQL 客户端构造角色持久化实现。
     *
     * @param sql Jimmer 的 {@link JSqlClient}，由 Spring 容器注入
     */
    public RolePortImpl(JSqlClient sql) {
        super(sql);
    }

    /**
     * 返回此仓储对应的 Jimmer 表定义对象，供父类 {@link CommonRepository} 使用。
     *
     * @return {@link SysRoleTable} 表定义
     */
    @Override
    public AbstractTypedTable<SysRole> getTable() {
        return table;
    }

    /**
     * 创建新角色记录（仅插入模式）。
     *
     * @param fetcher 对象抓取器，控制返回的 {@link SysRole} 加载哪些关联字段
     * @param input   角色输入数据（Jimmer 动态实体）
     * @return 创建成功后的角色实体
     */
    @Override
    public SysRole create(Fetcher<SysRole> fetcher, Input<SysRole> input) {
        return saveCommand(input)
                .setMode(SaveMode.INSERT_ONLY)
                .execute(fetcher)
                .getModifiedEntity();
    }

    /**
     * 更新角色信息，并追加角色与数据范围的关联关系。
     *
     * <p>使用 {@code APPEND} 关联保存模式，新增的 {@code roleIdScopes}
     * 会追加到现有关联中，不会删除已有关联记录。</p>
     *
     * @param input 角色更新输入，包含角色基本信息及可选的 {@code roleIdScopes} 列表
     * @return {@code true} 表示数据已修改并持久化，{@code false} 表示无变更
     */
    @Override
    public Boolean update(RoleUpdateInput input) {
        // APPEND 模式：仅追加新关联，不删除已有角色-范围映射
        return sql.saveCommand(input)
                .setAssociatedMode(SysRoleProps.ROLE_ID_SCOPES, AssociatedSaveMode.APPEND)
                .execute()
                .isModified();
    }

    /**
     * 统计给定角色 ID 列表中可见（未被过滤）的角色数量。
     *
     * <p>用于校验待操作角色是否存在且对当前上下文可见。
     * "可见"受 Jimmer 过滤拦截器（如多租户、数据权限）影响。</p>
     *
     * @param roleIds 角色 ID 列表，不可为 {@code null}
     * @return 可见的角色数量
     */
    @Override
    public long countVisible(@NonNull List<Long> roleIds) {
        return createQuery().where(table.id()
                        .in(roleIds))
                .selectCount()
                .execute()
                .getFirst();
    }

    /**
     * 分页查询角色列表，支持动态规格过滤。
     *
     * @param fetcher       对象抓取器
     * @param pageReq       分页请求参数
     * @param specification 角色查询规格，用于构建动态过滤条件
     * @return Jimmer 分页结果
     */
    @Override
    public Page<SysRole> select(Fetcher<SysRole> fetcher, PageReq pageReq, RoleSpecification specification) {
        return super.select(fetcher, pageReq, specification);
    }
}
