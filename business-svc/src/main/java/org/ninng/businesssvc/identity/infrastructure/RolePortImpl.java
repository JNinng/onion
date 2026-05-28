package org.ninng.businesssvc.identity.infrastructure;

import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTypedTable;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.identity.application.dto.RoleSpecification;
import org.ninng.businesssvc.identity.application.dto.RoleUpdateInput;
import org.ninng.businesssvc.identity.domain.model.SysRole;
import org.ninng.businesssvc.identity.domain.model.SysRoleProps;
import org.ninng.businesssvc.identity.domain.model.SysRoleTable;
import org.ninng.businesssvc.identity.domain.port.RolePort;
import org.ninng.businesssvc.model.filter.CommandDataScopeFilter;
import org.ninng.businesssvc.repository.CommonRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RolePortImpl extends CommonRepository<SysRole, Long> implements RolePort {

    private static final SysRoleTable table = SysRoleTable.$;

    public RolePortImpl(JSqlClient sql) {
        super(sql);
    }

    @Override
    public AbstractTypedTable<SysRole> getTable() {
        return table;
    }

    @Override
    public SysRole create(Fetcher<SysRole> fetcher, Input<SysRole> input) {
        return saveCommand(input)
                .setMode(SaveMode.INSERT_ONLY)
                .execute(fetcher)
                .getModifiedEntity();
    }

    @Override
    public Boolean update(RoleUpdateInput input) {
        return sql.saveCommand(input)
                .setOptimisticLock(SysRoleTable.class, new CommandDataScopeFilter<>())
                .setAssociatedMode(SysRoleProps.ROLE_ID_SCOPES, AssociatedSaveMode.APPEND)
                .execute()
                .isModified();
    }

    @Override
    public List<Long> selectVisible() {
        return List.of();
    }

    @Override
    public Page<SysRole> select(Fetcher<SysRole> fetcher, PageReq pageReq, RoleSpecification specification) {
        return super.select(fetcher, pageReq, specification);
    }
}
