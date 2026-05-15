package org.ninng.businesssvc.model.filter;

import org.babyfish.jimmer.sql.event.EntityEvent;
import org.babyfish.jimmer.sql.filter.CacheableFilter;
import org.babyfish.jimmer.sql.filter.FilterArgs;
import org.ninng.businesssvc.context.UserContextHolder;
import org.ninng.businesssvc.model.common.TenantAwareProps;
import org.springframework.stereotype.Component;

import java.util.SortedMap;
import java.util.TreeMap;

@Component
public class TenantFilter implements CacheableFilter<TenantAwareProps> {

    @Override
    public SortedMap<String, Object> getParameters() {
        TreeMap<String, Object> map = new TreeMap<>();
        map.put("tenantId", UserContextHolder.getTenantId());
        return map;
    }

    @Override
    public boolean isAffectedBy(EntityEvent<?> e) {
        return e.isChanged(TenantAwareProps.TENANT_ID);
    }

    @Override
    public void filter(FilterArgs<TenantAwareProps> args) {
        args.where(args.getTable()
                .tenantId()
                .eqIf(UserContextHolder.getTenantId()));
    }
}
