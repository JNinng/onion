package org.ninng.businesssvc.identity.domain.port;

import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jspecify.annotations.NonNull;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.identity.application.dto.RoleSpecification;
import org.ninng.businesssvc.identity.application.dto.RoleUpdateInput;
import org.ninng.businesssvc.identity.domain.model.SysRole;

import java.util.List;

/**
 * 角色端口接口。
 * <p>
 * <p>在洋葱架构中属于领域层的端口定义，定义应用层对角色聚合根（{@link SysRole}）所需的
 * 数据操作契约。具体实现由基础设施层的适配器提供，应用层仅依赖此接口，不感知底层存储技术。
 * <p>
 * <p>主要职责：
 * <ul>
 * <li>角色创建与更新</li>
 * <li>角色分页查询</li>
 * <li>角色可见性校验（用于权限变更前的合法性检查）</li>
 * </ul>
 */
public interface RolePort {

    /**
     * 创建一个新角色。
     * <p>
     * <p>前置条件：传入的 {@code input} 必须通过 Jimmer 实体的校验规则，角色编码不可重复。
     * <p>后置条件：返回的 {@link SysRole} 对象已包含生成的角色 ID 及所有请求字段。
     *
     * @param fetcher Jimmer 对象抓取器，指定需要加载的字段
     * @param input   角色实体输入对象，包含待创建的角色数据
     * @return 新创建的角色实体，字段按 {@code fetcher} 指定的范围填充
     */
    SysRole create(Fetcher<SysRole> fetcher, Input<SysRole> input);

    /**
     * 更新已有角色的信息。
     * <p>
     * <p>前置条件：{@code input} 中的角色 ID 必须对应一个已存在的角色。
     * <p>后置条件：角色的指定字段被更新为 {@code input} 中的值。
     * <p>只更新 {@link RoleUpdateInput} 中非 {@code null} 的字段，实现部分更新语义。
     *
     * @param input 角色更新输入对象，包含角色 ID 及待更新的字段
     * @return {@code true} 更新成功；{@code false} 角色不存在或更新失败
     */
    Boolean update(RoleUpdateInput input);

    /**
     * 统计当前用户可见的角色数量。
     * <p>
     * <p>用于在角色变更操作前进行权限校验，确保操作的角色均在该用户的可见范围内。
     * <p>前置条件：{@code changeRoleIds} 列表非 {@code null}。
     * <p>后置条件：返回的数量等于 {@code changeRoleIds} 的大小时，表示所有目标角色均可见。
     *
     * @param changeRoleIds 待校验的角色 ID 列表，不可为 {@code null}
     * @return 在当前用户租户和数据权限范围内可见的角色数量
     */
    long countVisible(@NonNull List<Long> changeRoleIds);

    /**
     * 分页查询角色列表。
     * <p>
     * <p>支持通过 {@link RoleSpecification} 进行多条件筛选，结果按分页参数 {@link PageReq} 返回。
     * <p>查询自动应用租户隔离：系统角色全局可见，租户角色仅对其所属租户可见。
     *
     * @param fetcher       Jimmer 对象抓取器，指定需要加载的字段
     * @param pageReq       分页请求参数（页码、每页条数、排序）
     * @param specification 角色查询规格，包含筛选条件（如角色编码、角色类型、状态等）
     * @return 分页的角色实体列表
     */
    Page<SysRole> select(Fetcher<SysRole> fetcher, PageReq pageReq, RoleSpecification specification);
}
