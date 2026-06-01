package org.ninng.businesssvc.identity.domain.type;

import org.babyfish.jimmer.sql.EnumItem;
import org.babyfish.jimmer.sql.EnumType;

/**
 * 数据范围规则枚举，定义角色可访问数据的可见范围。
 *
 * <p>该枚举通过 Jimmer 的 {@code @EnumType(Strategy.ORDINAL)} 以序数持久化到数据库，
 * 包含从"仅本人"到"全租户"共 6 个层级的数据范围。每个枚举值通过
 * {@link #toType()} 方法映射到对应的密封类型实例，用于策略模式分发数据权限校验逻辑。
 *
 * <p>内部定义了 {@link DataScopeType} 密封接口及其 6 个实现类，
 * 形成枚举名称与类型化行为之间的双向绑定：枚举 → 类型（{@code toType()}），
 * 类型 → 枚举（{@code toEnum()}）。
 *
 * @see DataScopeType
 */
@EnumType(EnumType.Strategy.ORDINAL)
public enum DataScope {

    /**
     * 仅本人：只能查看与自己相关的数据（如本人创建或本人负责的记录）
     */
    @EnumItem(ordinal = 1)
    PERSONAL,

    /**
     * 本部门：可查看所属部门范围内的数据
     */
    @EnumItem(ordinal = 2)
    DEPARTMENT,

    /**
     * 本部门及子部门：可查看所属部门及其所有下级子部门的数据
     */
    @EnumItem(ordinal = 3)
    DEPARTMENT_AND_SUBDEPARTMENT,

    /**
     * 指定人：可查看特定用户（一人或多人）范围内的数据
     */
    @EnumItem(ordinal = 4)
    SPECIFIED,

    /**
     * 指定部门：可查看特定部门（一个或多个）范围内的数据
     */
    @EnumItem(ordinal = 5)
    SPECIFIED_DEPT,

    /**
     * 全租户：可查看当前租户下的所有数据，无范围限制
     */
    @EnumItem(ordinal = 6)
    ALL_TENANT;

    /**
     * 将当前枚举值转换为对应的 {@link DataScopeType} 密封类型实例。
     *
     * <p>每种数据范围都对应一个单例类型对象，
     * 便于在策略模式中通过 {@code instanceof} 或模式匹配进行分流处理。
     *
     * @return 与当前枚举值绑定的 {@code DataScopeType} 单例实例
     */
    public DataScopeType toType() {
        // 使用 switch 表达式完成枚举到密封类型的映射，编译器会检查完整性
        return switch (this) {
            case PERSONAL -> PersonalType.INSTANCE;
            case DEPARTMENT -> DepartmentType.INSTANCE;
            case DEPARTMENT_AND_SUBDEPARTMENT -> DepartmentAndSubType.INSTANCE;
            case SPECIFIED -> SpecifiedType.INSTANCE;
            case SPECIFIED_DEPT -> SpecifiedDeptType.INSTANCE;
            case ALL_TENANT -> AllTenantType.INSTANCE;
        };
    }

    /**
     * 数据范围类型的密封接口，作为策略模式中的类型标记。
     *
     * <p>该接口是 {@code sealed} 的，其许可的实现类覆盖了所有 6 种数据范围。
     * 外部代码可以通过模式匹配 {@code switch (type)} 安全地处理所有情况，
     * 无需担心遗漏分支。
     *
     * <p>每个实现类都是 {@code final} 的静态内部类，通过私有构造器 + 公开静态常量
     * {@code INSTANCE} 实现单例，确保无内存浪费。
     *
     * @see PersonalType
     * @see DepartmentType
     * @see DepartmentAndSubType
     * @see SpecifiedType
     * @see SpecifiedDeptType
     * @see AllTenantType
     */
    public sealed interface DataScopeType
            permits PersonalType, DepartmentType,
            DepartmentAndSubType, SpecifiedType,
            SpecifiedDeptType, AllTenantType {

        /**
         * 将当前类型实例反向映射回对应的 {@link DataScope} 枚举值。
         *
         * @return 与当前类型绑定的枚举常量
         */
        DataScope toEnum();
    }

    /**
     * "仅本人"数据范围类型，对应 {@link DataScope#PERSONAL}。
     * <p>表示用户只能查看与自己直接关联的数据。
     */
    public static final class PersonalType implements DataScopeType {
        /**
         * 单例实例
         */
        public static final PersonalType INSTANCE = new PersonalType();

        private PersonalType() {
        }

        @Override
        public DataScope toEnum() {
            return DataScope.PERSONAL;
        }
    }

    /**
     * "本部门"数据范围类型，对应 {@link DataScope#DEPARTMENT}。
     * <p>表示用户可查看所属直接部门的数据。
     */
    public static final class DepartmentType implements DataScopeType {
        /**
         * 单例实例
         */
        public static final DepartmentType INSTANCE = new DepartmentType();

        private DepartmentType() {
        }

        @Override
        public DataScope toEnum() {
            return DataScope.DEPARTMENT;
        }
    }

    /**
     * "本部门及子部门"数据范围类型，对应 {@link DataScope#DEPARTMENT_AND_SUBDEPARTMENT}。
     * <p>表示用户可查看所属部门及其所有下级子部门的数据。
     */
    public static final class DepartmentAndSubType implements DataScopeType {
        /**
         * 单例实例
         */
        public static final DepartmentAndSubType INSTANCE = new DepartmentAndSubType();

        private DepartmentAndSubType() {
        }

        @Override
        public DataScope toEnum() {
            return DataScope.DEPARTMENT_AND_SUBDEPARTMENT;
        }
    }

    /**
     * "指定人"数据范围类型，对应 {@link DataScope#SPECIFIED}。
     * <p>表示用户可查看指定用户（一人或多人）范围内的数据。
     */
    public static final class SpecifiedType implements DataScopeType {
        /**
         * 单例实例
         */
        public static final SpecifiedType INSTANCE = new SpecifiedType();

        private SpecifiedType() {
        }

        @Override
        public DataScope toEnum() {
            return DataScope.SPECIFIED;
        }
    }

    /**
     * "指定部门"数据范围类型，对应 {@link DataScope#SPECIFIED_DEPT}。
     * <p>表示用户可查看指定部门（一个或多个）范围内的数据。
     */
    public static final class SpecifiedDeptType implements DataScopeType {
        /**
         * 单例实例
         */
        public static final SpecifiedDeptType INSTANCE = new SpecifiedDeptType();

        private SpecifiedDeptType() {
        }

        @Override
        public DataScope toEnum() {
            return DataScope.SPECIFIED_DEPT;
        }
    }

    /**
     * "全租户"数据范围类型，对应 {@link DataScope#ALL_TENANT}。
     * <p>表示用户可查看当前租户下的所有数据，无范围限制。
     */
    public static final class AllTenantType implements DataScopeType {
        /**
         * 单例实例
         */
        public static final AllTenantType INSTANCE = new AllTenantType();

        private AllTenantType() {
        }

        @Override
        public DataScope toEnum() {
            return DataScope.ALL_TENANT;
        }
    }
}
