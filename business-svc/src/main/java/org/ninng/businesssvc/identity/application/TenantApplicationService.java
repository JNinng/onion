package org.ninng.businesssvc.identity.application;

import jakarta.validation.constraints.NotNull;
import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.identity.application.dto.TenantCreateInput;
import org.ninng.businesssvc.identity.application.dto.TenantSpecification;
import org.ninng.businesssvc.identity.domain.model.SysTenant;
import org.ninng.businesssvc.identity.domain.port.TenantPort;
import org.ninng.businesssvc.utils.IdUtils;
import org.springframework.stereotype.Service;

/**
 * 租户应用服务，负责处理租户管理的业务用例，包括创建、按编码查找、删除和分页查询。
 * <p>
 * <p>该服务协调调用 {@link TenantPort} 端口接口完成数据操作。
 * 在创建租户时，会自动调用 {@link IdUtils#generateTenantCode()} 生成 16 位 Base32 的唯一租户编码，
 * 确保租户编码的全局唯一性和不可预测性。
 * <p>
 * <p>主要用例：
 * <ul>
 * <li>创建租户（自动生成租户编码）</li>
 * <li>按租户编码查找租户</li>
 * <li>按 ID 删除租户（软删除）</li>
 * <li>分页查询租户列表</li>
 * </ul>
 *
 * @see TenantPort
 * @see IdUtils
 */
@Service
public class TenantApplicationService {

    private final TenantPort tenantPort;

    public TenantApplicationService(TenantPort tenantPort) {
        this.tenantPort = tenantPort;
    }

    /**
     * 创建租户，自动生成全局唯一的租户编码。
     * <p>
     * <p>租户编码由 {@link IdUtils#generateTenantCode()} 通过 Feistel 置换算法生成，
     * 为 16 位 Base32 字符串，具有不可预测性和全局唯一性。
     *
     * @param fetcher Jimmer 抓取器，用于控制返回的 {@link SysTenant} 对象加载哪些关联字段
     * @param input   租户创建输入，包含租户名称等信息
     * @return 创建成功后的 {@link SysTenant} 实体，按 {@code fetcher} 指定的字段加载
     */
    public SysTenant create(Fetcher<SysTenant> fetcher, TenantCreateInput input) {
        // 自动生成 16 位 Base32 的唯一租户编码
        input.setCode(IdUtils.generateTenantCode());
        return tenantPort.create(fetcher, input);
    }

    /**
     * 按租户编码查找租户。
     *
     * @param fetcher    Jimmer 抓取器，用于控制返回的 {@link SysTenant} 对象加载哪些关联字段
     * @param tenantCode 租户编码，不能为空
     * @return 匹配的 {@link SysTenant} 实体，未找到则返回 {@code null}
     */
    public SysTenant findByCode(Fetcher<SysTenant> fetcher, @NotNull String tenantCode) {
        return tenantPort.findByCode(fetcher, tenantCode);
    }

    /**
     * 按 ID 删除租户（软删除）。
     *
     * @param id 租户 ID
     * @return {@code true} 删除成功；{@code false} 删除失败或租户不存在
     */
    public Boolean removeById(String id) {
        return tenantPort.removeById(id);
    }

    /**
     * 分页查询租户列表，支持按条件筛选。
     *
     * @param fetcher       Jimmer 抓取器，用于控制返回的 {@link SysTenant} 对象加载哪些关联字段
     * @param pageReq       分页请求参数，包含页码和每页条数
     * @param specification 租户查询规格，包含可选的筛选条件（如租户名称、状态等）
     * @return 分页结果 {@link Page}，包含租户列表和分页信息
     */
    public Page<SysTenant> list(Fetcher<SysTenant> fetcher, PageReq pageReq,
                                TenantSpecification specification) {
        return tenantPort.list(fetcher, pageReq, specification);
    }
}
