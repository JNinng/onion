package org.ninng.businesssvc.repository;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTypedTable;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.model.SysRole;
import org.ninng.businesssvc.model.SysRoleFetcher;
import org.ninng.businesssvc.model.SysRoleTable;
import org.springframework.stereotype.Repository;

@Repository
public class RoleRepository extends CommonRepository<SysRole, Long> {

    private static final SysRoleTable table = SysRoleTable.$;

    private static final Fetcher<SysRole> DEFAULT_FETCHER = SysRoleFetcher.$.allScalarFields();

    public RoleRepository(JSqlClient sql) {
        super(sql);
    }

    @Override
    AbstractTypedTable<SysRole> getTable() {
        return table;
    }
}
