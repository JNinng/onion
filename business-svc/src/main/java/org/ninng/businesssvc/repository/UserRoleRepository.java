package org.ninng.businesssvc.repository;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.table.AssociationTable;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.ninng.businesssvc.constant.C;
import org.ninng.businesssvc.constant.CacheConstant;
import org.ninng.businesssvc.model.SysUser;
import org.ninng.businesssvc.model.SysUserTableEx;
import org.ninng.businesssvc.model.filter.TenantFilter;
import org.ninng.businesssvc.role.application.dto.RoleDetailsView;
import org.ninng.businesssvc.role.domain.model.SysRole;
import org.ninng.businesssvc.role.domain.model.SysRoleTableEx;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserRoleRepository {

    private final JSqlClient sqlClient;

    public UserRoleRepository(JSqlClient sqlClient) {
        this.sqlClient = sqlClient;
    }

    @NonNull
    @Cacheable(cacheNames = CacheConstant.USER_ROLE, key = "#userId")
    public List<RoleDetailsView> findByUserId(@Nullable Long userId) {
        return findByUserId(userId, false);
    }

    @NonNull
    @Cacheable(cacheNames = CacheConstant.USER_ROLE, key = "#userId")
    public List<RoleDetailsView> findByUserId(@Nullable Long userId, boolean disableTenant) {
        AssociationTable<SysUser, SysUserTableEx, SysRole, SysRoleTableEx> associationTable = AssociationTable.of(
                SysUserTableEx.class, SysUserTableEx::roles);
        try {
            List<RoleDetailsView> list = sqlClient.filters(it -> {
                        if (disableTenant) {
                            it.disableByTypes(TenantFilter.class);
                        }
                    })
                    .createAssociationQuery(associationTable)
                    .where(Predicate.and(associationTable.source()
                            .id()
                            .eq(userId)), associationTable.source()
                            .status()
                            .eq(C.Data.ENABLED), associationTable.target()
                            .status()
                            .eq(C.Data.ENABLED))
                    .select(associationTable.target()
                            .fetch(RoleDetailsView.class))
                    .execute();
            if (list == null) {
                return List.of();
            }
            return list;
        } catch (Exception e) {
            return List.of();
        }
    }
}
