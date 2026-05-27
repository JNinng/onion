package org.ninng.businesssvc.identity.infrastructure;

import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTypedTable;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.identity.domain.model.SysDept;
import org.ninng.businesssvc.identity.domain.model.SysDeptTable;
import org.ninng.businesssvc.identity.domain.port.DeptPort;
import org.ninng.businesssvc.identity.application.dto.DeptSpecification;
import org.ninng.businesssvc.repository.CommonRepository;
import org.springframework.stereotype.Repository;

@Repository
public class DeptPortImpl extends CommonRepository<SysDept, Long> implements DeptPort {

    private static final SysDeptTable table = SysDeptTable.$;

    public DeptPortImpl(JSqlClient sql) {
        super(sql);
    }

    @Override
    public AbstractTypedTable<SysDept> getTable() {
        return table;
    }

    @Override
    public SysDept create(Fetcher<SysDept> fetcher, Input<SysDept> input) {
        return saveCommand(input)
                .setMode(SaveMode.INSERT_ONLY)
                .execute(fetcher)
                .getModifiedEntity();
    }

    @Override
    public Page<SysDept> list(Fetcher<SysDept> fetcher, PageReq pageReq, DeptSpecification specification) {
        return super.select(fetcher, pageReq, specification);
    }

    @Override
    public Boolean deleteById(Long id) {
        return delete(id);
    }
}
