package org.ninng.businesssvc.context;

import com.alibaba.ttl.TransmittableThreadLocal;
import jakarta.annotation.Nullable;
import org.ninng.businesssvc.model.SysUser;

import java.util.UUID;

/**
 * 登录用户上下文
 */
public class UserContextHolder {

    private static final TransmittableThreadLocal<UUID> tenantId = new TransmittableThreadLocal<>();
    private static final TransmittableThreadLocal<SysUser> user = new TransmittableThreadLocal<>();

    public static void removes() {
        tenantId.remove();
        user.remove();
    }

    @Nullable
    public static UUID getTenantId() {
        return tenantId.get();
    }

    public static void setTenantId(@Nullable UUID tenantId) {
        UserContextHolder.tenantId.set(tenantId);
    }

    @Nullable
    public static SysUser getUser() {
        return user.get();
    }

    public static void setUser(@Nullable SysUser user) {
        UserContextHolder.user.set(user);
    }

    @Nullable
    public static UUID getUserId() {
        SysUser sysUser = user.get();
        if (sysUser == null) {
            return null;
        }
        return sysUser.id();
    }
}
