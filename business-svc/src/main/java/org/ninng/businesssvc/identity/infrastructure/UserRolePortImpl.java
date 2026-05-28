package org.ninng.businesssvc.identity.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.table.AssociationTable;
import org.jspecify.annotations.Nullable;
import org.ninng.businesssvc.constant.C;
import org.ninng.businesssvc.constant.CacheConstant;
import org.ninng.businesssvc.identity.application.dto.RoleDetailsView;
import org.ninng.businesssvc.identity.domain.model.SysRole;
import org.ninng.businesssvc.identity.domain.model.SysRoleTableEx;
import org.ninng.businesssvc.identity.domain.model.SysUser;
import org.ninng.businesssvc.identity.domain.model.SysUserTableEx;
import org.ninng.businesssvc.identity.domain.port.UserRolePort;
import org.ninng.businesssvc.model.filter.TenantFilter;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Slf4j
public class UserRolePortImpl implements UserRolePort {

    private final JSqlClient sqlClient;

    public UserRolePortImpl(JSqlClient sqlClient) {
        this.sqlClient = sqlClient;
    }

    @Override
    @Cacheable(cacheNames = CacheConstant.USER_ROLE, key = "#userId", unless = "#result==null")
    public List<RoleDetailsView> findByUserId(@Nullable Long userId) {
        return findByUserId(userId, false);
    }

    @Override
    @Cacheable(cacheNames = CacheConstant.USER_ROLE, key = "#userId", unless = "#result==null")
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
                    .where(Predicate.and(
                            associationTable.source()
                                    .id()
                                    .eq(userId),
                            associationTable.source()
                                    .status()
                                    .eq(C.Data.ENABLED),
                            associationTable.target()
                                    .status()
                                    .eq(C.Data.ENABLED)))
                    .select(associationTable.target()
                            .fetch(RoleDetailsView.class))
                    .execute();
            if (list == null) {
                return List.of();
            }
            return list;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return List.of();
        }
    }
}
