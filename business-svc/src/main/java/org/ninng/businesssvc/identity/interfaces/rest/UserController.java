package org.ninng.businesssvc.identity.interfaces.rest;

import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.entity.R;
import org.ninng.businesssvc.identity.application.UserApplicationService;
import org.ninng.businesssvc.identity.application.dto.UserSelectionView;
import org.ninng.businesssvc.identity.application.dto.UserSpecification;
import org.ninng.businesssvc.identity.application.dto.UserUpdateInput;
import org.ninng.businesssvc.identity.domain.model.SysRoleFetcher;
import org.ninng.businesssvc.identity.domain.model.SysRoleIdScopeFetcher;
import org.ninng.businesssvc.identity.domain.model.SysUser;
import org.ninng.businesssvc.identity.domain.model.SysUserFetcher;
import org.ninng.businesssvc.version.ApiVersion;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户 REST 控制器（API v1，测试接口支持 v1.6）。
 *
 * <p>负责用户（SysUser）的查询与更新操作，包括分页列表查询、更新用户信息及用户选择视图。
 * 所有接口均映射至 {@code /user} 路径下。
 *
 * @author JNinng
 */
@RestController
@RequestMapping("/user")
public class UserController {

    /**
     * 默认抓取器：加载用户所有标量字段（排除密码），
     * 并级联加载其关联的 {@code roles} 及角色下的 {@code roleIdScopes}（仅 {@code status} 和 {@code dataId} 字段）。
     */
    private static final Fetcher<SysUser> DEFAULT_FETCHER = SysUserFetcher.$.allScalarFields()
            .password(false)
            .roles(SysRoleFetcher.$.allScalarFields()
                    .roleIdScopes(SysRoleIdScopeFetcher.$.status()
                            .dataId()));

    private final UserApplicationService userApplicationService;

    /**
     * 构造用户控制器。
     *
     * @param userApplicationService 用户应用服务，处理用户查询、更新等业务逻辑
     */
    public UserController(UserApplicationService userApplicationService) {
        this.userApplicationService = userApplicationService;
    }

    /**
     * 分页查询用户列表。
     *
     * <p>HTTP 方法：POST，路径：{@code /user/list}。
     * 支持通过 {@link UserSpecification} 进行多条件筛选，结合 {@link PageReq} 实现分页。
     * 返回数据不包含密码字段，但包含角色及其数据范围关联。
     *
     * @param specification 用户查询条件（支持按名称、昵称、状态等筛选）
     * @param pageReq       分页请求参数（页码、每页条数等）
     * @return 用户分页列表，每条记录包含所有标量字段（敏感字段除外）
     */
    @PostMapping("list")
    public R<Page<@FetchBy("DEFAULT_FETCHER") SysUser>> list(@RequestBody UserSpecification specification,
                                                             PageReq pageReq) {
        return R.ok(userApplicationService.list(DEFAULT_FETCHER, pageReq, specification));
    }

    /**
     * 更新用户信息。
     *
     * <p>HTTP 方法：POST，路径：{@code /user/update}。
     * 可更新用户基本属性及角色关联。
     *
     * @param input 用户更新请求参数（基本信息、角色列表等）
     * @return {@code true} 表示更新成功
     */
    @PostMapping("update")
    public R<Boolean> update(@RequestBody UserUpdateInput input) {
        return R.ok(userApplicationService.update(input));
    }

    /**
     * 获取用户选择视图列表。
     *
     * <p>HTTP 方法：POST，路径：{@code /user/selections}。
     * 返回精简的用户列表供前端下拉选择使用。
     *
     * @return 用户选择视图列表
     */
    @PostMapping("selections")
    public R<List<UserSelectionView>> selections() {
        return R.ok(userApplicationService.selections());
    }

    /**
     * 测试接口（API v1.6）。
     *
     * <p>HTTP 方法：POST，路径：{@code /user/test}（需携带版本号 {@code 1.6}）。
     * 当前仅打印输入参数，供开发调试使用。
     *
     * @param input 用户更新请求参数
     * @return 空响应
     */
    @ApiVersion(value = "1.6")
    @PostMapping("test")
    public R<Void> test(@RequestBody UserUpdateInput input) {
        System.out.println(input);
        return R.ok(null);
    }

    /**
     * 已弃用的测试接口。
     *
     * <p>HTTP 方法：POST，路径：{@code /user/test}（默认版本）。
     * 该接口已被 {@code v1.6} 版本的 {@link #test(UserUpdateInput)} 取代。
     *
     * @param input 用户更新请求参数
     * @return 空响应
     * @deprecated 请使用 API v1.6 的 {@link #test(UserUpdateInput)} 替代
     */
    @PostMapping("test")
    @ApiVersion(deprecated = true)
    public R<Void> test1(@RequestBody UserUpdateInput input) {
        System.out.println(input);
        return R.ok(null);
    }
}
