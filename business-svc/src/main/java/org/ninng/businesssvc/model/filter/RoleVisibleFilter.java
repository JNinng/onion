package org.ninng.businesssvc.model.filter;

import lombok.val;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.filter.FilterArgs;
import org.ninng.businesssvc.context.UserContextHolder;
import org.ninng.businesssvc.context.UserContextMode;
import org.ninng.businesssvc.identity.application.dto.RoleDetailsView;
import org.ninng.businesssvc.identity.domain.model.SysRoleProps;
import org.ninng.businesssvc.identity.domain.model.TableExes;
import org.ninng.businesssvc.identity.domain.type.DataScope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RoleVisibleFilter implements Filter<SysRoleProps> {

    private final JSqlClient sqlClient;

    public RoleVisibleFilter(JSqlClient sqlClient) {
        this.sqlClient = sqlClient;
    }

    @Override
    public void filter(FilterArgs<SysRoleProps> args) {
        switch (UserContextHolder.getMode()) {
            case UserContextMode.DefaultType ignored -> {
                roleVisibleFilter(args);
            }
            case UserContextMode.DisabledType ignored -> {
            }
        }
    }

    private void roleVisibleFilter(FilterArgs<SysRoleProps> args) {
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
        val deptDataScope = table.dataScope()
                .in(List.of(DataScope.DEPARTMENT, DataScope.DEPARTMENT_AND_SUBDEPARTMENT));
        wheres[i++] = Predicate.and(deptDataScope, table.scopeDeptId()
                .isNotNull());

        List<Long> dept = new ArrayList<>();
        List<Long> subDept = new ArrayList<>();
        for (RoleDetailsView role : roles) {
            val scopeDeptId = role.getScopeDeptId();
            if (scopeDeptId == null) {
                continue;
            }
            if (DataScope.DEPARTMENT.equals(role.getDataScope())) {
                dept.add(scopeDeptId);
            } else if (DataScope.DEPARTMENT_AND_SUBDEPARTMENT.equals(role.getDataScope())) {
                subDept.add(scopeDeptId);
            }
        }
        if (!dept.isEmpty()) {
            wheres[i++] = Predicate.and(deptDataScope, table.scopeDeptId()
                    .in(dept));
        }
        for (Long deptId : subDept) {
            val closureTableEx = TableExes.SYS_DEPT_CLOSURE_TABLE_EX;
            wheres[i++] = Predicate.and(deptDataScope, table.scopeDeptId()
                    .in(sqlClient.createSubQuery(closureTableEx)
                            .where(closureTableEx.ancestorId()
                                    .eq(deptId))
                            .select(closureTableEx.descendantId())));
        }
        args.where(Predicate.or(wheres));
    }
}
