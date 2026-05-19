package org.ninng.businesssvc.model.filter;

import org.babyfish.jimmer.sql.event.EntityEvent;
import org.babyfish.jimmer.sql.filter.CacheableFilter;
import org.babyfish.jimmer.sql.filter.FilterArgs;
import org.ninng.businesssvc.context.UserContextHolder;
import org.ninng.businesssvc.model.common.OwnerAwareProps;
import org.ninng.businesssvc.model.t.DataScope;
import org.springframework.stereotype.Component;

import java.util.SortedMap;
import java.util.TreeMap;

@Component
public class DataScopeFilter implements CacheableFilter<OwnerAwareProps> {

    @Override
    public SortedMap<String, Object> getParameters() {
        TreeMap<String, Object> map = new TreeMap<>();
        DataScope scope = UserContextHolder.getDataScope();
        if (scope == null) {
            return map;
        }
        map.put("dataScope", scope.name());
        switch (scope) {
            case PERSONAL -> map.put("ownerUserId", UserContextHolder.getUserId());
            case DEPARTMENT, DEPARTMENT_AND_SUBDEPARTMENT ->
                    map.put("ownerDeptId", UserContextHolder.getDeptId());
            // ALL_TENANT, SPECIFIED, SPECIFIED_DEPT: only dataScope key
            default -> { /* nothing extra */ }
        }
        return map;
    }

    @Override
    public boolean isAffectedBy(EntityEvent<?> e) {
        return e.isChanged(OwnerAwareProps.OWNER_USER_ID)
                || e.isChanged(OwnerAwareProps.OWNER_DEPT_ID);
    }

    @Override
    public void filter(FilterArgs<OwnerAwareProps> args) {
        DataScope scope = UserContextHolder.getDataScope();
        if (scope == null || scope == DataScope.ALL_TENANT
                || scope == DataScope.SPECIFIED || scope == DataScope.SPECIFIED_DEPT) {
            return;
        }
        switch (scope) {
            case PERSONAL -> args.where(args.getTable()
                    .ownerUserId()
                    .eqIf(UserContextHolder.getUserId()));
            case DEPARTMENT, DEPARTMENT_AND_SUBDEPARTMENT -> args.where(args.getTable()
                    .ownerDeptId()
                    .eqIf(UserContextHolder.getDeptId()));
            default -> { /* no filter for unknown scopes */ }
        }
    }
}
