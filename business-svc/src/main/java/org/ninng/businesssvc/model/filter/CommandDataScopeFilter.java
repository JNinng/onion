package org.ninng.businesssvc.model.filter;

import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.mutation.UserOptimisticLock;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.ninng.businesssvc.context.UserContextHolder;
import org.ninng.businesssvc.model.common.OwnerAwareProps;
import org.ninng.businesssvc.model.common.TenantAwareProps;

import java.util.List;
import java.util.UUID;

public class CommandDataScopeFilter<E, T extends Table<E>> implements UserOptimisticLock<E, T> {

    private static @NonNull Predicate tenantFilter(TenantAwareProps table) {
        return table.tenantId()
                .eq(UserContextHolder.getTenantId());
    }

    private static <E, T extends Table<E>> @Nullable Predicate ownerFilter(T table) {
        return table instanceof OwnerAwareProps ownerAwareProps ? ownerAwareProps.ownerUserId()
                .in(List.of(UUID.randomUUID())) : null;
    }

    @Override
    public Predicate predicate(T table, ValueExpressionFactory<E> valueExpressionFactory) {
        return Predicate.and(tenantFilter((TenantAwareProps) table), ownerFilter(table));
    }
}
