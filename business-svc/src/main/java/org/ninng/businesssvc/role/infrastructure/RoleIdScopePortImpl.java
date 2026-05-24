package org.ninng.businesssvc.role.infrastructure;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTypedTable;
import org.ninng.businesssvc.repository.CommonRepository;
import org.ninng.businesssvc.role.domain.model.SysRoleIdScope;
import org.ninng.businesssvc.role.domain.model.SysRoleIdScopeTable;
import org.ninng.businesssvc.role.domain.port.RoleIdScopePort;
import org.springframework.stereotype.Repository;

@Repository
public class RoleIdScopePortImpl extends CommonRepository<SysRoleIdScope, Long> implements RoleIdScopePort {

    private static final SysRoleIdScopeTable table = SysRoleIdScopeTable.$;

    public RoleIdScopePortImpl(JSqlClient sql) {
        super(sql);
    }

    @Override
    public AbstractTypedTable<SysRoleIdScope> getTable() {
        return table;
    }
}
