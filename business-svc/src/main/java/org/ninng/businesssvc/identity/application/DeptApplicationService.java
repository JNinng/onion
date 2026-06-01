package org.ninng.businesssvc.identity.application;

import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.identity.application.dto.DeptCreateInput;
import org.ninng.businesssvc.identity.application.dto.DeptSpecification;
import org.ninng.businesssvc.identity.domain.model.SysDept;
import org.ninng.businesssvc.identity.domain.port.DeptPort;
import org.springframework.stereotype.Service;

/**
 * 部门应用服务，负责处理部门管理的业务用例，包括创建、分页查询和删除。
 * <p>
 * <p>该服务作为部门领域层的门面，协调调用 {@link DeptPort} 端口接口完成数据操作。
 * 所有业务逻辑委托给领域层的端口实现，应用服务本身负责编排调用流程。
 * <p>
 * <p>主要用例：
 * <ul>
 * <li>创建部门</li>
 * <li>分页查询部门列表</li>
 * <li>按 ID 删除部门（软删除）</li>
 * </ul>
 *
 * @see DeptPort
 */
@Service
public class DeptApplicationService {

    private final DeptPort deptPort;

    public DeptApplicationService(DeptPort deptPort) {
        this.deptPort = deptPort;
    }

    /**
     * 创建部门。
     *
     * @param fetcher Jimmer 抓取器，用于控制返回的 {@link SysDept} 对象加载哪些关联字段
     * @param input   部门创建输入，包含部门名称、父级部门等信息
     * @return 创建成功后的 {@link SysDept} 实体，按 {@code fetcher} 指定的字段加载
     */
    public SysDept create(Fetcher<SysDept> fetcher, DeptCreateInput input) {
        return deptPort.create(fetcher, input);
    }

    /**
     * 分页查询部门列表，支持按条件筛选。
     *
     * @param fetcher       Jimmer 抓取器，用于控制返回的 {@link SysDept} 对象加载哪些关联字段
     * @param pageReq       分页请求参数，包含页码和每页条数
     * @param specification 部门查询规格，包含可选的筛选条件（如部门名称、状态等）
     * @return 分页结果 {@link Page}，包含部门列表和分页信息
     */
    public Page<SysDept> list(Fetcher<SysDept> fetcher, PageReq pageReq, DeptSpecification specification) {
        return deptPort.list(fetcher, pageReq, specification);
    }

    /**
     * 按 ID 删除部门（软删除）。
     *
     * @param id 部门 ID
     * @return {@code true} 删除成功；{@code false} 删除失败或部门不存在
     */
    public Boolean removeById(Long id) {
        return deptPort.removeById(id);
    }
}
