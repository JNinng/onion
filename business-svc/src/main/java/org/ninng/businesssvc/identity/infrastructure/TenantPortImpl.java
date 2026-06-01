package org.ninng.businesssvc.identity.infrastructure;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTypedTable;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.identity.application.dto.TenantSpecification;
import org.ninng.businesssvc.identity.domain.model.SysTenant;
import org.ninng.businesssvc.identity.domain.model.SysTenantTable;
import org.ninng.businesssvc.identity.domain.port.TenantPort;
import org.ninng.businesssvc.repository.CommonRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 租户端口接口的 Jimmer 持久化实现。
 *
 * <p>继承 {@link CommonRepository} 复用通用 CRUD 和分页查询能力。
 * 租户删除采用<strong>软删除</strong>策略，仅设置 {@code deletedAt} 时间戳，
 * 而非物理删除记录，保留数据可追溯性。</p>
 *
 * @author onion
 */
@Repository
public class TenantPortImpl extends CommonRepository<SysTenant, String> implements TenantPort {

    /**
     * Jimmer 编译期生成的租户表定义
     */
    private static final SysTenantTable table = SysTenantTable.$;

    /**
     * 通过 Jimmer SQL 客户端构造租户持久化实现。
     *
     * @param sql Jimmer 的 {@link JSqlClient}，由 Spring 容器注入
     */
    public TenantPortImpl(JSqlClient sql) {
        super(sql);
    }

    /**
     * 返回此仓储对应的 Jimmer 表定义对象，供父类 {@link CommonRepository} 使用。
     *
     * @return {@link SysTenantTable} 表定义
     */
    @Override
    public AbstractTypedTable<SysTenant> getTable() {
        return table;
    }

    /**
     * 创建新租户记录（仅插入模式）。
     *
     * @param fetcher 对象抓取器，控制返回的 {@link SysTenant} 加载哪些关联字段
     * @param input   租户输入数据（Jimmer 动态实体）
     * @return 创建成功后的租户实体
     */
    @Override
    public SysTenant create(Fetcher<SysTenant> fetcher, Input<SysTenant> input) {
        return saveCommand(input)
                .setMode(SaveMode.INSERT_ONLY)
                .execute(fetcher)
                .getModifiedEntity();
    }

    /**
     * 按主键软删除租户。
     *
     * <p>不执行物理删除，仅将 {@code deletedAt} 字段设置为当前时间戳。
     * 配合 Jimmer 的 {@code @LogicalDelete} 过滤拦截器，后续查询将自动排除已软删除的租户。</p>
     *
     * @param id 租户主键 ID（16 字符 Base32 编码）
     * @return {@code true} 表示至少影响了一行记录，{@code false} 表示记录不存在或已删除
     */
    @Override
    public Boolean removeById(String id) {
        // 软删除：设置删除时间戳，由 Jimmer 逻辑删除过滤器自动屏蔽后续查询
        return withUpdated().where(table.id()
                        .eq(id))
                .set(table.deletedAt(), LocalDateTime.now())
                .execute() > 0;
    }

    /**
     * 根据租户编码精确查询租户信息。
     *
     * <p>租户编码（{@code code}）在系统中应保持唯一，此方法返回单条匹配记录。</p>
     *
     * @param fetcher 对象抓取器，控制返回的 {@link SysTenant} 加载哪些关联字段
     * @param code    租户编码，不可为 {@code null}
     * @return 匹配的租户实体，未找到时返回 {@code null}
     */
    @Nullable
    @Override
    public SysTenant findByCode(Fetcher<SysTenant> fetcher, @NotNull String code) {
        List<SysTenant> list = createQuery().where(table.code()
                        .eq(code))
                .select(table.fetch(fetcher))
                .limit(1)
                .execute();
        if (list.isEmpty()) {
            return null;
        }
        return list.getFirst();
    }

    /**
     * 分页查询租户列表，支持动态规格过滤。
     *
     * @param fetcher       对象抓取器
     * @param pageReq       分页请求参数
     * @param specification 租户查询规格，用于构建动态过滤条件
     * @return Jimmer 分页结果
     */
    @Override
    public Page<SysTenant> list(Fetcher<SysTenant> fetcher, PageReq pageReq, TenantSpecification specification) {
        return super.select(fetcher, pageReq, specification);
    }
}
