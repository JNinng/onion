package org.ninng.businesssvc.identity.application;

import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.identity.application.dto.RoleCreateInput;
import org.ninng.businesssvc.identity.application.dto.RoleSpecification;
import org.ninng.businesssvc.identity.application.dto.RoleUpdateInput;
import org.ninng.businesssvc.identity.domain.model.SysRole;
import org.ninng.businesssvc.identity.domain.port.RolePort;
import org.springframework.stereotype.Service;

/**
 * 角色应用服务，负责处理角色管理的业务用例，包括创建、更新和分页查询。
 * <p>
 * <p>该服务协调调用 {@link RolePort} 端口接口完成数据操作。
 * 在更新角色时，会处理角色与数据权限范围（{@code RoleIdScope}）的关联关系，
 * 将角色 ID 回填到每个权限范围记录中，以确保关联数据的一致性。
 * <p>
 * <p>主要用例：
 * <ul>
 * <li>创建角色</li>
 * <li>更新角色及其数据权限范围</li>
 * <li>分页查询角色列表</li>
 * </ul>
 *
 * @see RolePort
 */
@Service
public class RoleApplicationService {

    private final RolePort roleRepository;

    public RoleApplicationService(RolePort roleRepository) {
        this.roleRepository = roleRepository;
    }

    /**
     * 创建角色。
     *
     * @param fetcher Jimmer 抓取器，用于控制返回的 {@link SysRole} 对象加载哪些关联字段
     * @param input   角色创建输入，包含角色编码、类型、数据权限范围等信息
     * @return 创建成功后的 {@link SysRole} 实体，按 {@code fetcher} 指定的字段加载
     */
    public SysRole create(Fetcher<SysRole> fetcher, RoleCreateInput input) {
        return roleRepository.create(fetcher, input);
    }

    /**
     * 更新角色信息，并处理关联的数据权限范围（{@code RoleIdScope}）。
     * <p>
     * <p>如果更新输入中包含数据权限范围列表，则：
     * <ol>
     * <li>将当前角色 ID 回填到每个范围记录中，确保外键关联正确</li>
     * <li>清空输入中的范围列表，由端口层单独处理范围数据的持久化</li>
     * </ol>
     *
     * @param input 角色更新输入，包含角色 ID、新属性值及可选的数据权限范围列表
     * @return {@code true} 更新成功；{@code false} 更新失败
     */
    public Boolean update(RoleUpdateInput input) {
        var roleIdScopes = input.getRoleIdScopes();
        // 如果传入了数据权限范围列表，回填角色 ID 以确保关联一致性
        if (roleIdScopes != null) {
            roleIdScopes.forEach(scope -> scope.setRoleId(input.getId()));
        }
        // 清空范围列表，交由端口层独立处理关联数据的持久化
        input.setRoleIdScopes(null);
        return roleRepository.update(input);
    }

    /**
     * 分页查询角色列表，支持按条件筛选。
     *
     * @param fetcher       Jimmer 抓取器，用于控制返回的 {@link SysRole} 对象加载哪些关联字段
     * @param pageReq       分页请求参数，包含页码和每页条数
     * @param specification 角色查询规格，包含可选的筛选条件（如角色编码、角色类型等）
     * @return 分页结果 {@link Page}，包含角色列表和分页信息
     */
    public Page<SysRole> list(Fetcher<SysRole> fetcher, PageReq pageReq, RoleSpecification specification) {
        return roleRepository.select(fetcher, pageReq, specification);
    }
}
