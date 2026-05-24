package org.ninng.businesssvc.context;

import com.alibaba.ttl.TransmittableThreadLocal;
import org.jspecify.annotations.Nullable;
import org.ninng.businesssvc.model.dto.UserDetailsView;
import org.ninng.businesssvc.role.application.dto.RoleDetailsView;
import org.ninng.businesssvc.role.domain.type.DataScope;

import java.util.List;

/**
 * 登录用户上下文
 */
public class UserContextHolder {

    private static final TransmittableThreadLocal<String> tenantId = new TransmittableThreadLocal<>();
    private static final TransmittableThreadLocal<UserDetailsView> user = new TransmittableThreadLocal<>();
    private static final TransmittableThreadLocal<List<RoleDetailsView>> roles = new TransmittableThreadLocal<>();

    public static void removes() {
        tenantId.remove();
        user.remove();
        roles.remove();
    }

    @Nullable
    public static String getTenantId() {
        return tenantId.get();
    }

    public static void setTenantId(@Nullable String tenantId) {
        UserContextHolder.tenantId.set(tenantId);
    }

    @Nullable
    public static UserDetailsView getUser() {
        return user.get();
    }

    public static void setUser(@Nullable UserDetailsView user) {
        UserContextHolder.user.set(user);
    }

    @Nullable
    public static List<RoleDetailsView> getRoles() {
        return roles.get();
    }

    public static void setRoles(@Nullable List<RoleDetailsView> roles) {
        UserContextHolder.roles.set(roles);
    }

    @Nullable
    public static Long getUserId() {
        UserDetailsView sysUser = user.get();
        if (sysUser == null) {
            return null;
        }
        return sysUser.getId();
    }

    public static DataScope getDataScope() {
        return null;
    }

    public static Long getDeptId() {
        return null;
    }


}
