package org.ninng.businesssvc.identity.domain.type;

import org.babyfish.jimmer.sql.EnumItem;
import org.babyfish.jimmer.sql.EnumType;

/**
 * 数据范围规则枚举
 */
@EnumType(EnumType.Strategy.ORDINAL)
public enum DataScope {
    /**
     * 仅本人
     */
    @EnumItem(ordinal = 1)
    PERSONAL,
    /**
     * 本部门
     */
    @EnumItem(ordinal = 2)
    DEPARTMENT,
    /**
     * 本部门及子部门
     */
    @EnumItem(ordinal = 3)
    DEPARTMENT_AND_SUBDEPARTMENT,
    /**
     * 指定人
     */
    @EnumItem(ordinal = 4)
    SPECIFIED,
    /**
     * 指定部门
     */
    @EnumItem(ordinal = 5)
    SPECIFIED_DEPT,
    /**
     * 全租户
     */
    @EnumItem(ordinal = 6)
    ALL_TENANT;

    public DataScopeType toType() {
        return switch (this) {
            case PERSONAL -> PersonalType.INSTANCE;
            case DEPARTMENT -> DepartmentType.INSTANCE;
            case DEPARTMENT_AND_SUBDEPARTMENT -> DepartmentAndSubType.INSTANCE;
            case SPECIFIED -> SpecifiedType.INSTANCE;
            case SPECIFIED_DEPT -> SpecifiedDeptType.INSTANCE;
            case ALL_TENANT -> AllTenantType.INSTANCE;
        };
    }

    public sealed interface DataScopeType
            permits PersonalType, DepartmentType,
            DepartmentAndSubType, SpecifiedType,
            SpecifiedDeptType, AllTenantType {

        DataScope toEnum();
    }

    public static final class PersonalType implements DataScopeType {
        public static final PersonalType INSTANCE = new PersonalType();

        private PersonalType() {
        }

        @Override
        public DataScope toEnum() {
            return DataScope.PERSONAL;
        }
    }

    public static final class DepartmentType implements DataScopeType {
        public static final DepartmentType INSTANCE = new DepartmentType();

        private DepartmentType() {
        }

        @Override
        public DataScope toEnum() {
            return DataScope.DEPARTMENT;
        }
    }

    public static final class DepartmentAndSubType implements DataScopeType {
        public static final DepartmentAndSubType INSTANCE = new DepartmentAndSubType();

        private DepartmentAndSubType() {
        }

        @Override
        public DataScope toEnum() {
            return DataScope.DEPARTMENT_AND_SUBDEPARTMENT;
        }
    }

    public static final class SpecifiedType implements DataScopeType {
        public static final SpecifiedType INSTANCE = new SpecifiedType();

        private SpecifiedType() {
        }

        @Override
        public DataScope toEnum() {
            return DataScope.SPECIFIED;
        }
    }

    public static final class SpecifiedDeptType implements DataScopeType {
        public static final SpecifiedDeptType INSTANCE = new SpecifiedDeptType();

        private SpecifiedDeptType() {
        }

        @Override
        public DataScope toEnum() {
            return DataScope.SPECIFIED_DEPT;
        }
    }

    public static final class AllTenantType implements DataScopeType {
        public static final AllTenantType INSTANCE = new AllTenantType();

        private AllTenantType() {
        }

        @Override
        public DataScope toEnum() {
            return DataScope.ALL_TENANT;
        }
    }
}
