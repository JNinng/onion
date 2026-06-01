package org.ninng.businesssvc.identity.domain.port;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.identity.application.dto.TenantSpecification;
import org.ninng.businesssvc.identity.domain.model.SysTenant;

/**
 * 租户端口接口。
 * <p>
 * <p>在洋葱架构中属于领域层的端口定义，定义应用层对租户聚合根（{@link SysTenant}）所需的
 * 数据操作契约。具体实现由基础设施层的适配器提供，应用层仅依赖此接口，不感知底层存储技术。
 * <p>
 * <p>主要职责：
 * <ul>
 * <li>租户创建与逻辑删除</li>
 * <li>按编码查询租户（用于幂等性校验）</li>
 * <li>租户分页查询</li>
 * </ul>
 * <p>
 * <p>租户 ID 由 {@link org.ninng.businesssvc.utils.IdUtils} 生成，
 * 为 16 字符的 Base32 编码字符串。
 */
public interface TenantPort {

    /**
     * 创建一个新租户。
     * <p>
     * <p>前置条件：传入的 {@code input} 必须通过 Jimmer 实体的校验规则，
     * 租户编码（code）不可重复。
     * <p>后置条件：返回的 {@link SysTenant} 对象已包含生成的租户 ID（16 字符 Base32）及所有请求字段。
     *
     * @param fetcher Jimmer 对象抓取器，指定需要加载的字段
     * @param input   租户实体输入对象，包含待创建的租户数据
     * @return 新创建的租户实体，字段按 {@code fetcher} 指定的范围填充
     */
    SysTenant create(Fetcher<SysTenant> fetcher, org.babyfish.jimmer.Input<SysTenant> input);

    /**
     * 根据租户编码查询租户。
     * <p>
     * <p>用于在创建或更新操作前进行幂等性校验：通过编码检查租户是否已存在。
     *
     * @param fetcher Jimmer 对象抓取器，指定需要加载的字段
     * @param code    租户编码，不可为 {@code null}
     * @return 匹配的租户实体；若不存在则返回 {@code null}
     */
    @Nullable
    SysTenant findByCode(Fetcher<SysTenant> fetcher, @NotNull String code);

    /**
     * 根据 ID 逻辑删除租户（软删除）。
     * <p>
     * <p>前置条件：租户 ID 必须存在且未被标记为已删除。
     * <p>后置条件：租户被标记为删除状态，不再出现在常规查询结果中。
     *
     * @param id 待删除的租户 ID（16 字符 Base32 编码）
     * @return {@code true} 删除成功；{@code false} 租户不存在或删除失败
     */
    Boolean removeById(String id);

    /**
     * 分页查询租户列表。
     * <p>
     * <p>支持通过 {@link TenantSpecification} 进行多条件筛选，结果按分页参数 {@link PageReq} 返回。
     *
     * @param fetcher       Jimmer 对象抓取器，指定需要加载的字段
     * @param pageReq       分页请求参数（页码、每页条数、排序）
     * @param specification 租户查询规格，包含筛选条件（如租户名称、编码、状态等）
     * @return 分页的租户实体列表
     */
    Page<SysTenant> list(Fetcher<SysTenant> fetcher, PageReq pageReq, TenantSpecification specification);
}
