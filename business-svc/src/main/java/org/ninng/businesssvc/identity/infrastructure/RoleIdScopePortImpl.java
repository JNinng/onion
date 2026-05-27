package org.ninng.businesssvc.identity.infrastructure;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTypedTable;
import org.ninng.businesssvc.identity.domain.model.SysRoleIdScope;
import org.ninng.businesssvc.identity.domain.model.SysRoleIdScopeTable;
import org.ninng.businesssvc.identity.domain.port.RoleIdScopePort;
import org.ninng.businesssvc.repository.CommonRepository;
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
