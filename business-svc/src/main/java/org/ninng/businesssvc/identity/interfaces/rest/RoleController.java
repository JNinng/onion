package org.ninng.businesssvc.identity.interfaces.rest;

import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.entity.R;
import org.ninng.businesssvc.identity.application.RoleApplicationService;
import org.ninng.businesssvc.identity.application.dto.RoleCreateInput;
import org.ninng.businesssvc.identity.application.dto.RoleSpecification;
import org.ninng.businesssvc.identity.application.dto.RoleUpdateInput;
import org.ninng.businesssvc.identity.domain.model.SysRole;
import org.ninng.businesssvc.identity.domain.model.SysRoleFetcher;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 角色 REST 控制器（API v1）。
 *
 * <p>负责角色（SysRole）的 CRUD 操作，包括分页查询、创建及更新（含数据范围关联）。
 * 所有接口均映射至 {@code /role} 路径下。
 *
 * @author JNinng
 */
@RestController
@RequestMapping("/role")
public class RoleController {

    /**
     * 默认抓取器：加载角色的所有标量字段，用于列表、创建、更新等接口的返回。
     */
    private static final Fetcher<SysRole> DEFAULT_FETCHER = SysRoleFetcher.$.allScalarFields();

    private final RoleApplicationService roleApplicationService;

    /**
     * 构造角色控制器。
     *
     * @param roleApplicationService 角色应用服务，处理角色创建、查询、更新等业务逻辑
     */
    public RoleController(RoleApplicationService roleApplicationService) {
        this.roleApplicationService = roleApplicationService;
    }

    /**
     * 分页查询角色列表。
     *
     * <p>HTTP 方法：POST，路径：{@code /role/list}。
     * 支持通过 {@link RoleSpecification} 进行多条件筛选，结合 {@link PageReq} 实现分页。
     *
     * @param specification 角色查询条件（支持按编码、角色类型等筛选）
     * @param pageReq       分页请求参数（页码、每页条数等）
     * @return 角色分页列表，每条记录包含所有标量字段
     */
    @PostMapping("list")
    public R<Page<@FetchBy("DEFAULT_FETCHER") SysRole>> list(@RequestBody RoleSpecification specification,
                                                             PageReq pageReq) {
        return R.ok(roleApplicationService.list(DEFAULT_FETCHER, pageReq, specification));
    }

    /**
     * 创建角色。
     *
     * <p>HTTP 方法：任意（{@code @RequestMapping}），路径：{@code /role/create}。
     *
     * @param input 角色创建请求参数（包含编码、名称、角色类型、数据范围等）
     * @return 创建成功后的角色完整信息
     */
    @RequestMapping("create")
    public R<@FetchBy("DEFAULT_FETCHER") SysRole> create(@RequestBody RoleCreateInput input) {
        return R.ok(roleApplicationService.create(DEFAULT_FETCHER, input));
    }

    /**
     * 更新角色信息（含数据范围关联）。
     *
     * <p>HTTP 方法：POST，路径：{@code /role/update}。
     * 若请求中包含 {@code roleIdScopes}，则自动回填角色 ID 以确保关联合规。
     *
     * @param input 角色更新请求参数（角色基本信息及可选的数据范围关联列表）
     * @return {@code true} 表示更新成功
     */
    @PostMapping("update")
    public R<Boolean> update(@RequestBody RoleUpdateInput input) {
        var roleIdScopes = input.getRoleIdScopes();
        // 补充 roleIdScopes 中的 roleId，确保每个数据范围关联都指向当前角色
        if (roleIdScopes != null) {
            roleIdScopes.forEach(scope -> scope.setRoleId(input.getId()));
        }
        return R.ok(roleApplicationService.update(input));
    }
}
