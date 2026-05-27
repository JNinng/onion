package org.ninng.businesssvc.model.filter;

import lombok.val;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.filter.FilterArgs;
import org.ninng.businesssvc.TableExes;
import org.ninng.businesssvc.context.UserContextHolder;
import org.ninng.businesssvc.identity.application.dto.RoleDetailsView;
import org.ninng.businesssvc.identity.domain.type.DataScope;
import org.ninng.businesssvc.model.common.OwnerAwareProps;
import org.springframework.stereotype.Component;

@Component
public class OwnerFilter implements Filter<OwnerAwareProps> {

    private final JSqlClient sqlClient;

    public OwnerFilter(JSqlClient sqlClient) {
        this.sqlClient = sqlClient;
    }

    @Override
    public void filter(FilterArgs<OwnerAwareProps> args) {
        if (UserContextHolder.isDisabled()) {
            return;
        }
        val roles = UserContextHolder.getRoles();
        val allTenant = roles.stream()
                .filter(roleDetailsView -> DataScope.ALL_TENANT.equals(roleDetailsView.getDataScope()))
                .findFirst();
        if (allTenant.isPresent()) {
            return;
        }
        val table = args.getTable();
        val wheres = new Predicate[roles.size()];
        int i = 0;
        for (RoleDetailsView role : roles) {
            switch (role.getDataScope()) {
                case PERSONAL -> wheres[i++] = table.ownerUserId()
                        .eq(UserContextHolder.getUserId());
                case DEPARTMENT -> wheres[i++] = table.ownerDeptId()
                        .eq(UserContextHolder.getDeptId());
                case DEPARTMENT_AND_SUBDEPARTMENT -> {
                    val closureTableEx = TableExes.SYS_DEPT_CLOSURE_TABLE_EX;
                    wheres[i++] = table.ownerDeptId()
                            .in(sqlClient.createSubQuery(closureTableEx)
                                    .where(closureTableEx.ancestorId()
                                            .eq(UserContextHolder.getDeptId()))
                                    .select(closureTableEx.descendantId()));
                }
                case SPECIFIED -> {
                    val scopeTableEx = TableExes.SYS_ROLE_ID_SCOPE_TABLE_EX;
                    wheres[i++] = table.ownerUserId()
                            .in(sqlClient.createSubQuery(scopeTableEx)
                                    .where(scopeTableEx.roleId()
                                            .eq(role.getId()))
                                    .select(scopeTableEx.dataId()));
                }
                case SPECIFIED_DEPT -> {
                    val scopeTableEx = TableExes.SYS_ROLE_ID_SCOPE_TABLE_EX;
                    wheres[i++] = table.ownerDeptId()
                            .in(sqlClient.createSubQuery(scopeTableEx)
                                    .where(scopeTableEx.roleId()
                                            .eq(role.getId()))
                                    .select(scopeTableEx.dataId()));
                }
            }
        }
        args.where(Predicate.or(wheres));
    }
}
