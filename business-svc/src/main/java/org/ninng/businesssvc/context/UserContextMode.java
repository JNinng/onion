package org.ninng.businesssvc.context;

/**
 * 用户上下文模式，控制 {@link UserContextHolder} 的行为。
 *
 * <ul>
 *   <li>{@link DefaultType} — 默认模式，owner/role 过滤生效，数据按角色权限范围过滤</li>
 *   <li>{@link DisabledType} — 禁用 owner/role 模式，跳过 owner/role 级数据过滤，用于认证/加载用户信息等场景</li>
 * </ul>
 *
 * @see UserContextHolder#getMode()
 */
public sealed interface UserContextMode permits UserContextMode.DefaultType, UserContextMode.DisabledType {

    final class DefaultType implements UserContextMode {
        public static final DefaultType INSTANCE = new DefaultType();

        private DefaultType() {
        }
    }

    final class DisabledType implements UserContextMode {
        public static final DisabledType INSTANCE = new DisabledType();

        private DisabledType() {
        }
    }
}
