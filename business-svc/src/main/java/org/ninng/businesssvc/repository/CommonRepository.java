package org.ninng.businesssvc.repository;

import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.Specification;
import org.babyfish.jimmer.View;
import org.babyfish.jimmer.spring.repo.support.AbstractJavaRepository;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.mutation.MutableUpdate;
import org.babyfish.jimmer.sql.ast.query.MutableRootQuery;
import org.babyfish.jimmer.sql.ast.query.specification.JSpecification;
import org.babyfish.jimmer.sql.ast.query.specification.PredicateApplier;
import org.babyfish.jimmer.sql.ast.query.specification.SpecificationArgs;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTypedTable;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.context.UserContextHolder;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.entity.exception.PermissionsException;
import org.ninng.businesssvc.model.common.CreatedAwareProps;
import org.ninng.businesssvc.model.common.UpdatedAwareProps;

import java.time.LocalDateTime;
import java.util.List;

public abstract class CommonRepository<E, ID> extends AbstractJavaRepository<E, ID> {

    public CommonRepository(JSqlClient sql) {
        super(sql);
    }

    /**
     * 构建更新语句，设置更新时间和更新人
     */
    public MutableUpdate withUpdated() throws ClassCastException {
        UpdatedAwareProps updatedAwareProps = getUpdatedTable();
        return sql.createUpdate(getUpdatedTable())
                .set(updatedAwareProps.updatedAt(), LocalDateTime.now())
                .set(updatedAwareProps.updatedBy(), UserContextHolder.getUserId());
    }

    public Boolean delete(ID id) throws ClassCastException {
        return withUpdated().where(getUpdatedTable().getId()
                        .eq(id))
                .set(getUpdatedTable().deletedAt(), LocalDateTime.now())
                .execute() > 0;
    }

    public <V extends View<E>> List<V> select(Class<V> viewClass) {
        return createQuery().orderBy(getCreatedTable().createdAt()
                        .desc())
                .select(getTable().fetch(viewClass))
                .execute();
    }

    public <V extends View<E>> List<V> select(Class<V> viewClass, Specification<E> specification) {
        return createQuery().where(specification)
                .orderBy(getCreatedTable().createdAt()
                        .desc())
                .select(getTable().fetch(viewClass))
                .execute();
    }

    public <V extends View<E>> List<V> select(Class<V> viewClass, Predicate... predicates) {
        return createQuery().where(predicates)
                .orderBy(getCreatedTable().createdAt()
                        .desc())
                .select(getTable().fetch(viewClass))
                .execute();
    }

    public List<E> select(Fetcher<E> fetcher, Predicate... predicates) {
        return createQuery().where(predicates)
                .orderBy(getCreatedTable().createdAt()
                        .desc())
                .select(getTable().fetch(fetcher))
                .execute();
    }

    public <T extends TableLike<E>> Page<E> select(Fetcher<E> fetcher, PageReq pageReq,
                                                   JSpecification<E, T> specification) {
        return createQuery().where(specification)
                .orderBy(getCreatedTable().createdAt()
                        .desc())
                .select(getTable().fetch(fetcher))
                .fetchPage(pageReq.getPageIndex(), pageReq.getPageSize());
    }

    public Page<E> select(Fetcher<E> fetcher, PageReq pageReq, Predicate... predicates) {
        return createQuery().where(predicates)
                .orderBy(getCreatedTable().createdAt()
                        .desc())
                .select(getTable().fetch(fetcher))
                .fetchPage(pageReq.getPageIndex(), pageReq.getPageSize());
    }

    /**
     * 当符合 {@link org.ninng.businesssvc.model.common.OwnerAware} 时，检验数据权限
     *
     * @throws PermissionsException 数据权限异常
     */
    public void verifyPermissions(Long userId, List<ID> dataIds) throws PermissionsException {
        System.out.println(userId);
    }

    /**
     * 将 Specification 转换为 Table，用于构建查询语句
     */
    public <T extends TableLike<E>> T specificationToTable(JSpecification<E, T> specification) {
        SpecificationArgs<E, T> args = new SpecificationArgs<>(new PredicateApplier(createQuery()));
        specification.applyTo(args);
        return args.getTable();
    }

    public MutableRootQuery<AbstractTypedTable<E>> createQuery() {
        return sql.createQuery(getTable());
    }

    abstract AbstractTypedTable<E> getTable();

    @SuppressWarnings("unchecked")
    public <T extends CreatedAwareProps> T getCreatedTable() throws ClassCastException {
        return (T) getTable();
    }

    @SuppressWarnings("unchecked")
    public <T extends UpdatedAwareProps> T getUpdatedTable() throws ClassCastException {
        return (T) getTable();
    }
}
