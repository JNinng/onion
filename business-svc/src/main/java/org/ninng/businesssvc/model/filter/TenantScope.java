package org.ninng.businesssvc.model.filter;

import org.babyfish.jimmer.sql.filter.AssociationIntegrityAssuranceFilter;
import org.babyfish.jimmer.sql.filter.FilterArgs;
import org.ninng.businesssvc.context.UserContextHolder;
import org.ninng.businesssvc.model.common.TenantAwareProps;

public class TenantScope implements AssociationIntegrityAssuranceFilter<TenantAwareProps> {

    @Override
    public void filter(FilterArgs<TenantAwareProps> args) {
        args.where(args.getTable()
                .tenantId()
                .eqIf(UserContextHolder.getTenantId()));
    }
}
