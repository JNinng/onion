package org.ninng.businesssvc.identity.domain.port;

/**
 * 角色数据范围端口接口。
 * <p>
 * <p>在洋葱架构中属于领域层的端口定义，定义应用层对角色数据范围关联（
 * {@link org.ninng.businesssvc.identity.domain.model.SysRoleIdScope}）所需的
 * 数据操作契约。具体实现由基础设施层的适配器提供。
 * <p>
 * <p>该端口用于管理角色与数据范围之间的多对多关联关系，
 * 支持按角色维度查询其授权的数据范围，以及按数据范围维度查询关联的角色。
 */
public interface RoleIdScopePort {
}
