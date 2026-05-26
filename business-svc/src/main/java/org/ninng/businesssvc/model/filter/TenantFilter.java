package org.ninng.businesssvc.model.filter;

import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.filter.FilterArgs;
import org.ninng.businesssvc.context.UserContextHolder;
import org.ninng.businesssvc.model.common.TenantAwareProps;
import org.springframework.stereotype.Component;

@Component
public class TenantFilter implements Filter<TenantAwareProps> {

    @Override
    public void filter(FilterArgs<TenantAwareProps> args) {
        args.where(args.getTable()
                .tenantId()
                .eqIf(UserContextHolder.getTenantId()));
    }
}
