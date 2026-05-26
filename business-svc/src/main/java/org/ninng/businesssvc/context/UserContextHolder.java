package org.ninng.businesssvc.context;

import com.alibaba.ttl.TransmittableThreadLocal;
import org.jspecify.annotations.NonNull;
import org.ninng.businesssvc.model.dto.UserDetailsView;
import org.ninng.businesssvc.role.application.dto.RoleDetailsView;

import java.util.List;
import java.util.function.Supplier;

/**
 * 登录用户上下文，基于 {@link TransmittableThreadLocal} 的请求级上下文持有者。
 *
 * <h3>两层上下文模型</h3>
 * <ul>
 *   <li><b>真实上下文（real）</b> — 由认证过滤器（{@code DatabaseUserDetailsService}）设置，
 *       代表实际登录用户身份，用于审计追踪。</li>
 *   <li><b>影子上下文（shadow）</b> — 用于子租户代理操作场景，父租户管理员可临时以子租户身份
 *       执行操作，影子信息覆盖真实信息参与权限判断。</li>
 * </ul>
 *
 * <h3>读取优先级（由 {@code shadowMode} 标记控制）</h3>
 * <ul>
 *   <li>{@link #isShadow()} 为 {@code true} → 返回影子值（即使为 {@code null}），绝不穿透到真实上下文</li>
 *   <li>{@link #isShadow()} 为 {@code false} → 返回真实值</li>
 * </ul>
 * <p>审计场景请使用 {@code getReal*()} 方法获取真实登录身份，不受影子模式影响。
 *
 * <h3>快照与恢复</h3>
 * <p>后台异步任务可通过 {@link #snapshot()} 捕获当前完整上下文（含真实、影子及 {@code shadowMode} 标记），
 * 在异步执行线程中通过 {@link #restore(Snapshot)} 恢复现场，确保跨线程上下文传递。
 *
 * <h3>生命周期</h3>
 * <p>每次 HTTP 请求结束时，{@code UserContextOnceFilterHandler} 调用 {@link #removes()}
 * 清除所有 ThreadLocal，防止内存泄漏。
 *
 * @see com.alibaba.ttl.TransmittableThreadLocal
 */
public class UserContextHolder {

    /**
     * 用与区分禁用 user dept role，不作用于 tenantId
     */
    private static final TransmittableThreadLocal<Boolean> disabled = new TransmittableThreadLocal<>();

    private static final TransmittableThreadLocal<String> realTenantId = new TransmittableThreadLocal<>();
    private static final TransmittableThreadLocal<UserDetailsView> realUser = new TransmittableThreadLocal<>();
    private static final TransmittableThreadLocal<Long> realDeptId = new TransmittableThreadLocal<>();
    private static final TransmittableThreadLocal<List<RoleDetailsView>> realRoles = new TransmittableThreadLocal<>();

    private static final TransmittableThreadLocal<Boolean> shadowMode = new TransmittableThreadLocal<>();
    private static final TransmittableThreadLocal<String> shadowTenantId = new TransmittableThreadLocal<>();
    private static final TransmittableThreadLocal<UserDetailsView> shadowUser = new TransmittableThreadLocal<>();
    private static final TransmittableThreadLocal<Long> shadowDeptId = new TransmittableThreadLocal<>();
    private static final TransmittableThreadLocal<List<RoleDetailsView>> shadowRoles = new TransmittableThreadLocal<>();

    /**
     * 清除当前线程的所有上下文（真实 + 影子 + 模式标记），防止内存泄漏。
     *
     * <p>由过滤器链在每次请求完成后调用。
     */
    public static void removes() {
        disabled.remove();
        realTenantId.remove();
        realUser.remove();
        realDeptId.remove();
        realRoles.remove();
        exitShadow();
    }

    public static boolean isDisabled() {
        return Boolean.TRUE.equals(disabled.get());
    }

    public static void setDisabled(boolean disabled) {
        UserContextHolder.disabled.set(disabled);
    }

    public static void disable() {
        UserContextHolder.setDisabled(true);
    }

    public static void enable() {
        UserContextHolder.setDisabled(false);
    }

    public static <T> T callWithDisabled(Supplier<T> supplier) {
        UserContextHolder.disable();
        try {
            return supplier.get();
        } finally {
            UserContextHolder.enable();
        }
    }

    /**
     * 获取当前有效的租户 ID。
     *
     * <p>返回规则：
     * <ul>
     *   <li>影子模式（{@link #isShadow()} = {@code true}）→ 返回影子租户 ID</li>
     *   <li>非影子模式 → 返回真实租户 ID</li>
     * </ul>
     *
     * @return 有效租户 ID，可能为 {@code null}
     */
    @NonNull
    public static String getTenantId() {
        if (isShadow()) {
            return shadowTenantId.get();
        }
        return realTenantId.get();
    }

    /**
     * 设置真实租户 ID。
     *
     * <p>由认证流程（{@code DatabaseUserDetailsService}）调用，
     * 写入当前登录用户所属的租户标识。
     *
     * @param tenantId 租户 ID，可为 {@code null}
     */
    public static void setTenantId(@NonNull String tenantId) {
        realTenantId.set(tenantId);
    }

    /**
     * 获取真实租户 ID，不受影子模式影响。
     *
     * <p>始终返回认证时写入的真实值，用于审计日志等需要记录实际操作人身份的场景。
     *
     * @return 真实租户 ID，可能为 {@code null}
     */
    @NonNull
    public static String getRealTenantId() {
        return realTenantId.get();
    }

    /**
     * 设置影子租户 ID 并自动进入影子模式。
     *
     * <p>调用后 {@link #isShadow()} 返回 {@code true}，
     * 所有 {@code get*()} 方法返回影子信息。
     *
     * @param tenantId 代理目标租户 ID
     */
    public static void setShadowTenantId(@NonNull String tenantId) {
        shadowMode.set(Boolean.TRUE);
        shadowTenantId.set(tenantId);
    }

    /**
     * 获取当前有效的用户信息。
     *
     * <p>返回规则：
     * <ul>
     *   <li>影子模式（{@link #isShadow()} = {@code true}）→ 返回影子用户</li>
     *   <li>非影子模式 → 返回真实用户</li>
     * </ul>
     *
     * @return 有效用户视图，可能为 {@code null}
     */
    @NonNull
    public static UserDetailsView getUser() {
        if (isShadow()) {
            return shadowUser.get();
        }
        return realUser.get();
    }

    /**
     * 设置真实用户信息。
     *
     * <p>由认证流程调用，写入当前登录用户的完整视图。
     *
     * @param user 用户视图，可为 {@code null}
     */
    public static void setUser(@NonNull UserDetailsView user) {
        realUser.set(user);
    }

    /**
     * 获取当前有效的用户 ID。
     *
     * <p>等价于 {@code getUser().getId()}，受影子模式控制。
     *
     * @return 有效用户 ID，可能为 {@code null}
     */
    @NonNull
    public static Long getUserId() {
        return getUser().getId();
    }

    /**
     * 获取真实用户信息，不受影子模式影响。
     *
     * <p>始终返回认证时写入的真实登录用户，用于审计日志。
     *
     * @return 真实用户视图，可能为 {@code null}
     */
    @NonNull
    public static UserDetailsView getRealUser() {
        return realUser.get();
    }

    /**
     * 获取真实用户 ID，不受影子模式影响。
     *
     * <p>等价于 {@code getRealUser().getId()}，用于审计追踪。
     *
     * @return 真实用户 ID，可能为 {@code null}
     */
    @NonNull
    public static Long getRealUserId() {
        return realUser.get()
                .getId();
    }

    /**
     * 设置影子用户信息并自动进入影子模式。
     *
     * <p>调用后 {@link #isShadow()} 返回 {@code true}，
     * 所有 {@code get*()} 方法返回影子信息。
     *
     * @param user 代理目标用户视图
     */
    public static void setShadowUser(@NonNull UserDetailsView user) {
        shadowMode.set(Boolean.TRUE);
        shadowUser.set(user);
    }

    /**
     * 获取当前有效的部门 ID。
     *
     * <p>返回规则：
     * <ul>
     *   <li>影子模式（{@link #isShadow()} = {@code true}）→ 返回影子部门 ID</li>
     *   <li>非影子模式 → 返回真实部门 ID</li>
     * </ul>
     *
     * @return 有效部门 ID，可能为 {@code null}
     */
    @NonNull
    public static Long getDeptId() {
        if (isShadow()) {
            return shadowDeptId.get();
        }
        return realDeptId.get();
    }

    /**
     * 设置真实部门 ID。
     *
     * <p>由认证流程调用，写入当前登录用户所属的部门标识。
     *
     * @param deptId 部门 ID，可为 {@code null}
     */
    public static void setDeptId(@NonNull Long deptId) {
        realDeptId.set(deptId);
    }

    /**
     * 获取真实部门 ID，不受影子模式影响。
     *
     * <p>始终返回认证时写入的真实值，用于审计日志。
     *
     * @return 真实部门 ID，可能为 {@code null}
     */
    @NonNull
    public static Long getRealDeptId() {
        return realDeptId.get();
    }

    /**
     * 设置影子部门 ID 并自动进入影子模式。
     *
     * <p>调用后 {@link #isShadow()} 返回 {@code true}，
     * 所有 {@code get*()} 方法返回影子信息。
     *
     * @param deptId 代理目标部门 ID
     */
    public static void setShadowDeptId(@NonNull Long deptId) {
        shadowMode.set(Boolean.TRUE);
        shadowDeptId.set(deptId);
    }

    /**
     * 获取当前有效的角色列表。
     *
     * <p>返回规则：
     * <ul>
     *   <li>影子模式（{@link #isShadow()} = {@code true}）→ 返回影子角色列表</li>
     *   <li>非影子模式 → 返回真实角色列表</li>
     * </ul>
     *
     * @return 有效角色列表，可能为 {@code null}
     */
    @NonNull
    public static List<RoleDetailsView> getRoles() {
        if (isShadow()) {
            return shadowRoles.get();
        }
        return realRoles.get();
    }

    /**
     * 设置真实角色列表。
     *
     * <p>由认证流程调用，写入当前登录用户拥有的角色集合。
     *
     * @param roles 角色列表，可为 {@code null}
     */
    public static void setRoles(@NonNull List<RoleDetailsView> roles) {
        realRoles.set(roles);
    }

    /**
     * 获取真实角色列表，不受影子模式影响。
     *
     * @return 真实角色列表，可能为 {@code null}
     */
    @NonNull
    public static List<RoleDetailsView> getRealRoles() {
        return realRoles.get();
    }

    /**
     * 设置影子角色列表并自动进入影子模式。
     *
     * <p>调用后 {@link #isShadow()} 返回 {@code true}，
     * 所有 {@code get*()} 方法返回影子信息。
     * 影子角色的 {@code dataScope} 决定代理操作的数据范围。
     *
     * @param roles 代理目标角色列表
     */
    public static void setShadowRoles(@NonNull List<RoleDetailsView> roles) {
        shadowMode.set(Boolean.TRUE);
        shadowRoles.set(roles);
    }

    /**
     * 一键进入影子模式，同时设置租户、用户、部门、角色四个维度的影子上下文。
     *
     * <p>典型场景：父租户管理员查看/操作子租户数据时，传入子租户的身份信息。
     * 调用后 {@link #isShadow()} 返回 {@code true}。
     *
     * @param tenantId 代理目标租户 ID
     * @param user     代理目标用户视图
     * @param deptId   代理目标部门 ID
     * @param roles    代理目标角色列表
     */
    public static void enterShadow(@NonNull String tenantId, @NonNull UserDetailsView user, @NonNull Long deptId,
                                   @NonNull List<RoleDetailsView> roles) {
        shadowMode.set(Boolean.TRUE);
        shadowTenantId.set(tenantId);
        shadowUser.set(user);
        shadowDeptId.set(deptId);
        shadowRoles.set(roles);
    }

    /**
     * 退出影子模式，清除影子标记和所有影子上下文。
     *
     * <p>调用后 {@link #isShadow()} 返回 {@code false}，
     * 所有 {@code get*()} 方法回退到返回真实上下文。
     * 重复调用无副作用。
     */
    public static void exitShadow() {
        shadowMode.remove();
        shadowTenantId.remove();
        shadowUser.remove();
        shadowDeptId.remove();
        shadowRoles.remove();
    }

    /**
     * 判断当前是否处于影子模式。
     *
     * <p>由 {@link #shadowMode} 标记决定，不依赖各字段是否为 {@code null}，
     * 确保影子模式下所有 getter 行为一致，不会部分穿透到真实上下文。
     *
     * @return {@code true} 当前处于影子模式
     */
    public static boolean isShadow() {
        return Boolean.TRUE.equals(shadowMode.get());
    }

    /**
     * 创建当前上下文的完整快照（含真实、影子及模式标记）。
     *
     * <p>用于后台异步任务：提交任务前在主线程调用此方法捕获上下文，
     * 任务执行时在线程内部调用 {@link #restore(Snapshot)} 恢复现场。
     *
     * <pre>{@code
     * Snapshot saved = UserContextHolder.snapshot();
     * CompletableFuture.runAsync(() -> {
     *     UserContextHolder.restore(saved);
     *     // ... 业务逻辑，此时 getTenantId()/getUser()/isShadow() 与原线程一致
     * }).thenRun(() -> UserContextHolder.removes());
     * }</pre>
     *
     * @return 不可变快照，包含当前线程的全部上下文
     */
    public static Snapshot snapshot() {
        return new Snapshot(realTenantId.get(), realUser.get(), realDeptId.get(), realRoles.get(), isShadow(),
                shadowTenantId.get(), shadowUser.get(), shadowDeptId.get(), shadowRoles.get());
    }

    /**
     * 从快照恢复上下文，覆盖当前线程的全部真实、影子信息及模式标记。
     *
     * <p>传入 {@code null} 时静默返回，不做任何操作。
     *
     * @param snapshot 由 {@link #snapshot()} 创建的快照，可为 {@code null}
     */
    public static void restore(@NonNull Snapshot snapshot) {
        realTenantId.set(snapshot.realTenantId);
        realUser.set(snapshot.realUser);
        realDeptId.set(snapshot.realDeptId);
        realRoles.set(snapshot.realRoles);
        if (snapshot.shadowMode) {
            shadowMode.set(Boolean.TRUE);
            shadowTenantId.set(snapshot.shadowTenantId);
            shadowUser.set(snapshot.shadowUser);
            shadowDeptId.set(snapshot.shadowDeptId);
            shadowRoles.set(snapshot.shadowRoles);
        } else {
            exitShadow();
        }
    }

    /**
     * 上下文快照，包含真实和影子信息及影子模式标记的完整拷贝。
     *
     * <p>由 {@link UserContextHolder#snapshot()} 创建，通过
     * {@link UserContextHolder#restore(Snapshot)} 恢复。
     * 快照是不可变的点-in-time 副本，可跨线程传递。
     */
    public static class Snapshot {

        private final @NonNull String realTenantId;
        private final @NonNull UserDetailsView realUser;
        private final @NonNull Long realDeptId;
        private final @NonNull List<RoleDetailsView> realRoles;
        private final boolean shadowMode;
        private final @NonNull String shadowTenantId;
        private final @NonNull UserDetailsView shadowUser;
        private final @NonNull Long shadowDeptId;
        private final @NonNull List<RoleDetailsView> shadowRoles;

        private Snapshot(@NonNull String realTenantId, @NonNull UserDetailsView realUser, @NonNull Long realDeptId,
                         @NonNull List<RoleDetailsView> realRoles, boolean shadowMode, @NonNull String shadowTenantId,
                         @NonNull UserDetailsView shadowUser, @NonNull Long shadowDeptId,
                         @NonNull List<RoleDetailsView> shadowRoles) {
            this.realTenantId = realTenantId;
            this.realUser = realUser;
            this.realDeptId = realDeptId;
            this.realRoles = realRoles;
            this.shadowMode = shadowMode;
            this.shadowTenantId = shadowTenantId;
            this.shadowUser = shadowUser;
            this.shadowDeptId = shadowDeptId;
            this.shadowRoles = shadowRoles;
        }

        /**
         * 获取快照中的真实租户 ID。
         */
        @NonNull
        public String getRealTenantId() {
            return realTenantId;
        }

        /**
         * 获取快照中的真实用户信息。
         */
        @NonNull
        public UserDetailsView getRealUser() {
            return realUser;
        }

        /**
         * 获取快照中的真实部门 ID。
         */
        @NonNull
        public Long getRealDeptId() {
            return realDeptId;
        }

        /**
         * 获取快照中的真实角色列表。
         */
        @NonNull
        public List<RoleDetailsView> getRealRoles() {
            return realRoles;
        }

        /**
         * 获取快照中的影子租户 ID。
         *
         * @return 影子租户 ID，非影子模式下为 {@code null}
         */
        @NonNull
        public String getShadowTenantId() {
            return shadowTenantId;
        }

        /**
         * 获取快照中的影子用户信息。
         *
         * @return 影子用户视图，非影子模式下为 {@code null}
         */
        @NonNull
        public UserDetailsView getShadowUser() {
            return shadowUser;
        }

        /**
         * 获取快照中的影子部门 ID。
         *
         * @return 影子部门 ID，非影子模式下为 {@code null}
         */
        @NonNull
        public Long getShadowDeptId() {
            return shadowDeptId;
        }

        /**
         * 获取快照中的影子角色列表。
         *
         * @return 影子角色列表，非影子模式下为 {@code null}
         */
        @NonNull
        public List<RoleDetailsView> getShadowRoles() {
            return shadowRoles;
        }

        /**
         * 快照创建时是否处于影子模式。
         *
         * @return {@code true} 快照包含影子上下文
         */
        public boolean isShadowMode() {
            return shadowMode;
        }
    }
}
