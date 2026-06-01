package org.ninng.businesssvc.identity.infrastructure;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTypedTable;
import org.ninng.businesssvc.identity.domain.model.SysRoleIdScope;
import org.ninng.businesssvc.identity.domain.model.SysRoleIdScopeTable;
import org.ninng.businesssvc.identity.domain.port.RoleIdScopePort;
import org.ninng.businesssvc.repository.CommonRepository;
import org.springframework.stereotype.Repository;

/**
 * 角色 ID 范围端口接口的 Jimmer 持久化实现。
 *
 * <p>继承 {@link CommonRepository} 复用通用 CRUD 能力，
 * 为 {@code SysRoleIdScope}（角色与数据范围关联）提供基础持久化支持。
 * 复杂的关联查询由领域服务 {@code RoleScopeAuthorizationService} 负责，
 * 本类仅提供表定义注册，确保父类查询方法能正确解析实体表映射。</p>
 *
 * @author onion
 */
@Repository
public class RoleIdScopePortImpl extends CommonRepository<SysRoleIdScope, Long> implements RoleIdScopePort {

    /**
     * Jimmer 编译期生成的角色范围关联表定义
     */
    private static final SysRoleIdScopeTable table = SysRoleIdScopeTable.$;

    /**
     * 通过 Jimmer SQL 客户端构造角色范围持久化实现。
     *
     * @param sql Jimmer 的 {@link JSqlClient}，由 Spring 容器注入
     */
    public RoleIdScopePortImpl(JSqlClient sql) {
        super(sql);
    }

    /**
     * 返回此仓储对应的 Jimmer 表定义对象，供父类 {@link CommonRepository} 使用。
     *
     * @return {@link SysRoleIdScopeTable} 表定义
     */
    @Override
    public AbstractTypedTable<SysRoleIdScope> getTable() {
        return table;
    }
}
