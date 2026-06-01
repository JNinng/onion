package org.ninng.businesssvc.identity.interfaces.rest;

import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.entity.R;
import org.ninng.businesssvc.identity.application.TenantApplicationService;
import org.ninng.businesssvc.identity.application.dto.TenantCreateInput;
import org.ninng.businesssvc.identity.application.dto.TenantSpecification;
import org.ninng.businesssvc.identity.domain.model.SysTenant;
import org.ninng.businesssvc.identity.domain.model.SysTenantFetcher;
import org.springframework.web.bind.annotation.*;

/**
 * 租户 REST 控制器（API v1）。
 *
 * <p>负责租户（SysTenant）的 CRUD 操作，包括创建、分页列表查询、按编码查询及逻辑删除。
 * 所有接口均映射至 {@code /tenant} 路径下。
 *
 * @author JNinng
 */
@RestController
@RequestMapping("/tenant")
public class TenantController {

    /**
     * 详情/删除接口的默认抓取器：加载所有标量字段，排除 {@code deletedAt} 软删除时间戳。
     */
    private static final Fetcher<SysTenant> DEFAULT_FETCHER = SysTenantFetcher.$.allScalarFields()
            .deletedAt(false);
    /**
     * 创建接口的抓取器：创建成功后仅返回 {@code name} 和 {@code code} 字段，避免暴露多余信息。
     */
    private static final Fetcher<SysTenant> CREATE_FETCHER = SysTenantFetcher.$.name()
            .code();

    private final TenantApplicationService tenantApplicationService;

    /**
     * 构造租户控制器。
     *
     * @param tenantApplicationService 租户应用服务，处理租户创建、查询、删除等业务逻辑
     */
    public TenantController(TenantApplicationService tenantApplicationService) {
        this.tenantApplicationService = tenantApplicationService;
    }

    /**
     * 创建租户。
     *
     * <p>HTTP 方法：POST，路径：{@code /tenant/create}。
     * 创建成功后仅返回租户编码和名称。
     *
     * @param input 租户创建请求参数（包含编码、名称等）
     * @return 创建成功后的租户信息（仅含 {@code name} 和 {@code code}）
     */
    @PostMapping("create")
    public R<@FetchBy("CREATE_FETCHER") SysTenant> create(@RequestBody TenantCreateInput input) {
        return R.ok(tenantApplicationService.create(CREATE_FETCHER, input));
    }

    /**
     * 分页查询租户列表。
     *
     * <p>HTTP 方法：POST，路径：{@code /tenant/list}。
     * 支持通过 {@link TenantSpecification} 进行多条件筛选，结合 {@link PageReq} 实现分页。
     *
     * @param specification 租户查询条件（支持按编码、名称等筛选）
     * @param pageReq       分页请求参数（页码、每页条数等）
     * @return 租户分页列表，每条记录包含所有标量字段（软删除字段除外）
     */
    @PostMapping("list")
    public R<Page<@FetchBy("CREATE_FETCHER") SysTenant>> list(@RequestBody TenantSpecification specification,
                                                              PageReq pageReq) {
        return R.ok(tenantApplicationService.list(DEFAULT_FETCHER, pageReq, specification));
    }

    /**
     * 按租户编码查询租户详情。
     *
     * <p>HTTP 方法：POST，路径：{@code /tenant/get/{tenantCode}}。
     *
     * @param tenantCode 租户编码（16 位 Base32，由 {@code TenantIdGenerator} 生成）
     * @return 租户完整信息（所有标量字段，软删除字段除外）
     */
    @PostMapping("/get/{tenantCode}")
    public R<@FetchBy("DEFAULT_FETCHER") SysTenant> get(@PathVariable String tenantCode) {
        return R.ok(tenantApplicationService.findByCode(DEFAULT_FETCHER, tenantCode));
    }

    /**
     * 删除租户（逻辑删除）。
     *
     * <p>HTTP 方法：POST，路径：{@code /tenant/delete:id}。
     * 仅标记删除，不物理删除数据库记录。
     *
     * @param id 租户 ID
     * @return {@code true} 表示删除成功
     */
    @PostMapping("/delete:{id}")
    public R<Boolean> delete(@PathVariable String id) {
        return R.ok(tenantApplicationService.removeById(id));
    }
}
