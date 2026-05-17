package org.ninng.businesssvc.repository;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.ast.query.specification.JSpecification;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTypedTable;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.model.SysTenant;
import org.ninng.businesssvc.model.SysTenantTable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class TenantRepository extends CommonRepository<SysTenant, String> {

    private static final SysTenantTable table = SysTenantTable.$;

    public TenantRepository(JSqlClient sql) {
        super(sql);
    }

    @Override
    AbstractTypedTable<SysTenant> getTable() {
        return table;
    }

    public SysTenant create(Fetcher<SysTenant> fetcher, @NotNull SysTenant sysTenant) {
        return sql.saveCommand(sysTenant)
                .setMode(SaveMode.INSERT_ONLY)
                .execute(fetcher)
                .getModifiedEntity();
    }

    /**
     * <pre>
     *      // saveCommand 无法触发租户数据（tenantId）约束
     *      return sql.saveCommand(Immutables.createSysTenant(draft -> {
     *                 draft.setId(id);
     *                 draft.setDeletedAt(LocalDateTime.now());
     *                 draft.setTenantId(UserContextHolder.getUserId());
     *             }))
     *             .setMode(SaveMode.UPDATE_ONLY)
     *             .execute()
     *             .isModified();
     * </pre>
     * <pre>
     *     // createUpdate 无法触发 update 相关字段
     *     return sql.createUpdate(table)
     *              .set(table.deletedAt(), LocalDateTime.now())
     *              .set(table.updatedBy(), UserContextHolder.getUserId())
     *              .where(table.id()
     *                  .eq(id))
     *              .execute() > 0;
     * </pre>
     */
    @Override
    public Boolean delete(String id) {
        return withUpdated().where(table.id()
                        .eq(id))
                .set(table.deletedAt(), LocalDateTime.now())
                .execute() > 0;
    }

    @Nullable
    public SysTenant findByCode(Fetcher<SysTenant> fetcher, @NotNull String code) {
        List<SysTenant> list = createQuery().where(table.code()
                        .eq(code))
                .select(table.fetch(fetcher))
                .limit(1)
                .execute();
        if (list.isEmpty()) {
            return null;
        }
        return list.getFirst();
    }

    public Page<SysTenant> list(Fetcher<SysTenant> fetcher, JSpecification<SysTenant, SysTenantTable> specification,
                                PageReq pageReq) {
        return select(fetcher, pageReq, specification);
    }
}
