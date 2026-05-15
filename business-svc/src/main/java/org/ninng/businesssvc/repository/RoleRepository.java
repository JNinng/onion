package org.ninng.businesssvc.repository;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTypedTable;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.model.SysRole;
import org.ninng.businesssvc.model.SysRoleFetcher;
import org.ninng.businesssvc.model.SysRoleTable;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class RoleRepository extends CommonRepository<SysRole, UUID> {

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
