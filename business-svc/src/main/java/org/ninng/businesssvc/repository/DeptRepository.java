package org.ninng.businesssvc.repository;

import jakarta.validation.constraints.NotNull;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTypedTable;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.model.SysDept;
import org.ninng.businesssvc.model.SysDeptTable;
import org.springframework.stereotype.Repository;

@Repository
public class DeptRepository extends CommonRepository<SysDept, Long> {

    private static final SysDeptTable table = SysDeptTable.$;

    public DeptRepository(JSqlClient sql) {
        super(sql);
    }

    public SysDept create(Fetcher<SysDept> fetcher, @NotNull SysDept sysDept) {
        return sql.saveCommand(sysDept)
                .setMode(SaveMode.INSERT_ONLY)
                .execute(fetcher)
                .getModifiedEntity();
    }

    @Override
    AbstractTypedTable<SysDept> getTable() {
        return table;
    }
}
