package org.ninng.businesssvc.identity.infrastructure;

import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTypedTable;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.identity.application.dto.DeptSpecification;
import org.ninng.businesssvc.identity.domain.model.SysDept;
import org.ninng.businesssvc.identity.domain.model.SysDeptTable;
import org.ninng.businesssvc.identity.domain.port.DeptPort;
import org.ninng.businesssvc.repository.CommonRepository;
import org.springframework.stereotype.Repository;

/**
 * 部门端口接口的 Jimmer 持久化实现。
 *
 * <p>继承 {@link CommonRepository} 复用通用 CRUD 和分页查询能力，
 * 通过 {@code SysDeptTable} 关联 Jimmer 编译期生成的表定义，
 * 实现 {@link DeptPort} 中定义的部门数据操作契约。</p>
 *
 * @author onion
 */
@Repository
public class DeptPortImpl extends CommonRepository<SysDept, Long> implements DeptPort {

    /**
     * Jimmer 编译期生成的部门表定义，用于类型安全的 SQL 构建
     */
    private static final SysDeptTable table = SysDeptTable.$;

    /**
     * 通过 Jimmer SQL 客户端构造部门持久化实现。
     *
     * @param sql Jimmer 的 {@link JSqlClient}，由 Spring 容器注入
     */
    public DeptPortImpl(JSqlClient sql) {
        super(sql);
    }

    /**
     * 返回此仓储对应的 Jimmer 表定义对象，供父类 {@link CommonRepository} 使用。
     *
     * @return {@link SysDeptTable} 表定义
     */
    @Override
    public AbstractTypedTable<SysDept> getTable() {
        return table;
    }

    /**
     * 创建新部门记录（仅插入，不更新已存在记录）。
     *
     * @param fetcher 对象抓取器，控制返回的 {@link SysDept} 对象加载哪些关联字段
     * @param input   部门输入数据（Jimmer 动态实体）
     * @return 创建成功后的部门实体（包含数据库生成的 ID 等字段）
     */
    @Override
    public SysDept create(Fetcher<SysDept> fetcher, Input<SysDept> input) {
        // 使用 INSERT_ONLY 模式确保只插入、不更新已有记录
        return saveCommand(input)
                .setMode(SaveMode.INSERT_ONLY)
                .execute(fetcher)
                .getModifiedEntity();
    }

    /**
     * 分页查询部门列表，支持动态规格过滤。
     *
     * @param fetcher       对象抓取器，控制返回对象的字段加载
     * @param pageReq       分页请求参数（页码、每页大小等）
     * @param specification 部门查询规格，用于构建动态过滤条件
     * @return Jimmer 分页结果，包含部门列表及分页元数据
     */
    @Override
    public Page<SysDept> list(Fetcher<SysDept> fetcher, PageReq pageReq, DeptSpecification specification) {
        return super.select(fetcher, pageReq, specification);
    }

    /**
     * 按主键删除部门记录。
     *
     * @param id 部门主键 ID
     * @return {@code true} 表示删除成功，{@code false} 表示删除失败或记录不存在
     */
    @Override
    public Boolean removeById(Long id) {
        return delete(id);
    }
}
