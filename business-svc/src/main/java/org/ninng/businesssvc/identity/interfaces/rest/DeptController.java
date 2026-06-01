package org.ninng.businesssvc.identity.interfaces.rest;

import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.entity.R;
import org.ninng.businesssvc.identity.application.DeptApplicationService;
import org.ninng.businesssvc.identity.application.dto.DeptCreateInput;
import org.ninng.businesssvc.identity.application.dto.DeptSpecification;
import org.ninng.businesssvc.identity.domain.model.SysDept;
import org.ninng.businesssvc.identity.domain.model.SysDeptFetcher;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 部门 REST 控制器（API v1）。
 *
 * <p>负责部门（SysDept）的 CRUD 操作，包括创建、分页列表查询及逻辑删除。
 * 所有接口均映射至 {@code /dept} 路径下。
 *
 * @author JNinng
 */
@RestController
@RequestMapping("/dept")
public class DeptController {

    /**
     * 列表/详情接口的默认抓取器：加载所有标量字段，排除 {@code deletedAt} 软删除时间戳。
     */
    private static final Fetcher<SysDept> DEFAULT_FETCHER = SysDeptFetcher.$.allScalarFields()
            .deletedAt(false);
    /**
     * 创建接口的抓取器：创建成功后仅返回 {@code name} 字段，避免暴露多余信息。
     */
    private static final Fetcher<SysDept> CREATE_FETCHER = SysDeptFetcher.$.name();

    private final DeptApplicationService deptApplicationService;

    /**
     * 构造部门控制器。
     *
     * @param deptApplicationService 部门应用服务，处理部门创建、查询、删除等业务逻辑
     */
    public DeptController(DeptApplicationService deptApplicationService) {
        this.deptApplicationService = deptApplicationService;
    }

    /**
     * 创建部门。
     *
     * <p>HTTP 方法：POST，路径：{@code /dept/create}。
     * 创建成功后仅返回部门名称字段。
     *
     * @param input 部门创建请求参数
     * @return 创建成功后的部门信息（仅含 {@code name}）
     */
    @PostMapping("create")
    public R<@FetchBy("CREATE_FETCHER") SysDept> create(@RequestBody @Validated DeptCreateInput input) {
        return R.ok(deptApplicationService.create(CREATE_FETCHER, input));
    }

    /**
     * 分页查询部门列表。
     *
     * <p>HTTP 方法：POST，路径：{@code /dept/list}。
     * 支持通过 {@link DeptSpecification} 进行多条件筛选，结合 {@link PageReq} 实现分页。
     *
     * @param specification 部门查询条件（支持按名称、负责人等筛选）
     * @param pageReq       分页请求参数（页码、每页条数等）
     * @return 部门分页列表，每条记录包含所有标量字段（软删除字段除外）
     */
    @PostMapping("list")
    public R<Page<@FetchBy("DEFAULT_FETCHER") SysDept>> list(
            @RequestBody @Validated DeptSpecification specification, @Validated PageReq pageReq) {
        return R.ok(deptApplicationService.list(DEFAULT_FETCHER, pageReq, specification));
    }

    /**
     * 删除部门（逻辑删除）。
     *
     * <p>HTTP 方法：POST，路径：{@code /dept/delete/{id}}。
     * 仅标记删除，不物理删除数据库记录。
     *
     * @param id 部门 ID
     * @return {@code true} 表示删除成功
     */
    @PostMapping("/delete/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(deptApplicationService.removeById(id));
    }
}
