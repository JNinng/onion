package org.ninng.businesssvc.identity.domain.port;

import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.identity.application.dto.DeptSpecification;
import org.ninng.businesssvc.identity.domain.model.SysDept;

/**
 * 部门端口接口。
 * <p>
 * <p>在洋葱架构中属于领域层的端口定义，定义应用层对部门聚合根（{@link SysDept}）所需的
 * 数据操作契约。具体实现由基础设施层的适配器提供，应用层仅依赖此接口，不感知底层存储技术。
 * <p>
 * <p>主要职责：
 * <ul>
 * <li>部门创建</li>
 * <li>部门分页查询</li>
 * <li>部门逻辑删除（软删除）</li>
 * </ul>
 */
public interface DeptPort {

    /**
     * 创建一个新部门。
     * <p>
     * <p>前置条件：传入的 {@code input} 必须通过 Jimmer 实体的校验规则。
     * <p>后置条件：返回的 {@link SysDept} 对象已包含生成的部门 ID 及所有请求字段。
     *
     * @param fetcher Jimmer 对象抓取器，指定需要加载的字段
     * @param input   部门实体输入对象，包含待创建的部门数据
     * @return 新创建的部门实体，字段按 {@code fetcher} 指定的范围填充
     */
    SysDept create(Fetcher<SysDept> fetcher, org.babyfish.jimmer.Input<SysDept> input);

    /**
     * 分页查询部门列表。
     * <p>
     * <p>支持通过 {@link DeptSpecification} 进行多条件筛选，结果按分页参数 {@link PageReq} 返回。
     * <p>查询自动应用租户隔离和数据权限过滤。
     *
     * @param fetcher       Jimmer 对象抓取器，指定需要加载的字段
     * @param pageReq       分页请求参数（页码、每页条数、排序）
     * @param specification 部门查询规格，包含筛选条件（如部门名称、状态等）
     * @return 分页的部门实体列表
     */
    Page<SysDept> list(Fetcher<SysDept> fetcher, PageReq pageReq, DeptSpecification specification);

    /**
     * 根据 ID 逻辑删除部门（软删除）。
     * <p>
     * <p>前置条件：部门 ID 必须存在且未被标记为已删除。
     * <p>后置条件：部门被标记为删除状态，不再出现在常规查询结果中。
     *
     * @param id 待删除的部门 ID
     * @return {@code true} 删除成功；{@code false} 部门不存在或删除失败
     */
    Boolean removeById(Long id);
}
