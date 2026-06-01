package org.ninng.businesssvc.identity.domain.model;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.GeneratedValue;
import org.babyfish.jimmer.sql.Id;
import org.ninng.businesssvc.model.TenantIdGenerator;
import org.ninng.businesssvc.model.common.CreatedAware;
import org.ninng.businesssvc.model.common.StatusAware;
import org.ninng.businesssvc.model.common.TenantAware;
import org.ninng.businesssvc.model.common.UpdatedAware;

/**
 * 系统租户实体
 * <p>
 * <p>多租户架构中的租户核心实体，代表一个独立的企业/组织实例。
 * 通过 {@link TenantAware} 混入实现租户级别的数据隔离，
 * 系统中所有业务数据（用户、角色、部门等）都归属于某个租户。</p>
 * <p>
 * <p>租户ID使用{@link TenantIdGenerator}生成16位Base32编码的唯一标识，
 * 基于Feistel网络置换算法，保证ID不可预测且全局唯一。</p>
 * <p>
 * <p>{@code code}字段为租户唯一业务编码，用于登录时标识租户身份，
 * 通常与HTTP请求头{@code X-Tenant-Id}对应。</p>
 * <p>
 * <p>混入特性：</p>
 * <ul>
 * <li>{@link CreatedAware} — 记录创建时间与创建人</li>
 * <li>{@link UpdatedAware} — 记录更新时间与更新人</li>
 * <li>{@link StatusAware} — 支持逻辑删除（软删除）</li>
 * <li>{@link TenantAware} — 自身实现租户感知接口，
 * 但租户实体的租户ID始终为{@code null}或自身ID（根租户概念）</li>
 * </ul>
 */
@Entity
public interface SysTenant extends CreatedAware, UpdatedAware, StatusAware, TenantAware {

    /**
     * 租户唯一标识
     * <p>
     * <p>使用{@link TenantIdGenerator}生成16位Base32编码的字符串ID，
     * 基于Feistel网络置换算法确保全局唯一性。</p>
     */
    @Id
    @GeneratedValue(generatorType = TenantIdGenerator.class)
    String id();

    /**
     * 租户名称（展示用）
     */
    String name();

    /**
     * 租户唯一编码
     * <p>
     * <p>用于登录、API调用等场景中标识租户身份的业务编码，
     * 对应HTTP请求头{@code X-Tenant-Id}。</p>
     */
    String code();
}
