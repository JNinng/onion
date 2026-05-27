package org.ninng.businesssvc.identity.infrastructure;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTypedTable;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.identity.domain.model.SysTenant;
import org.ninng.businesssvc.identity.domain.model.SysTenantTable;
import org.ninng.businesssvc.identity.domain.port.TenantPort;
import org.ninng.businesssvc.identity.application.dto.TenantSpecification;
import org.ninng.businesssvc.repository.CommonRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class TenantPortImpl extends CommonRepository<SysTenant, String> implements TenantPort {

    private static final SysTenantTable table = SysTenantTable.$;

    public TenantPortImpl(JSqlClient sql) {
        super(sql);
    }

    @Override
    public AbstractTypedTable<SysTenant> getTable() {
        return table;
    }

    @Override
    public SysTenant create(Fetcher<SysTenant> fetcher, Input<SysTenant> input) {
        return saveCommand(input)
                .setMode(SaveMode.INSERT_ONLY)
                .execute(fetcher)
                .getModifiedEntity();
    }

    @Override
    public Boolean deleteById(String id) {
        return withUpdated().where(table.id().eq(id))
                .set(table.deletedAt(), LocalDateTime.now())
                .execute() > 0;
    }

    @Nullable
    @Override
    public SysTenant findByCode(Fetcher<SysTenant> fetcher, @NotNull String code) {
        List<SysTenant> list = createQuery().where(table.code().eq(code))
                .select(table.fetch(fetcher))
                .limit(1)
                .execute();
        if (list.isEmpty()) {
            return null;
        }
        return list.getFirst();
    }

    @Override
    public Page<SysTenant> list(Fetcher<SysTenant> fetcher, PageReq pageReq, TenantSpecification specification) {
        return super.select(fetcher, pageReq, specification);
    }
}
