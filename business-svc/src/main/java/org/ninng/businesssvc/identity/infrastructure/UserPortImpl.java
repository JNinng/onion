package org.ninng.businesssvc.identity.infrastructure;

import jakarta.annotation.Nullable;
import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.View;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTypedTable;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.identity.application.dto.UserDetailsView;
import org.ninng.businesssvc.identity.application.dto.UserSpecification;
import org.ninng.businesssvc.identity.application.dto.UserUpdateInput;
import org.ninng.businesssvc.identity.domain.model.SysUser;
import org.ninng.businesssvc.identity.domain.model.SysUserTable;
import org.ninng.businesssvc.identity.domain.port.UserPort;
import org.ninng.businesssvc.model.filter.CommandDataScopeFilter;
import org.ninng.businesssvc.repository.CommonRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserPortImpl extends CommonRepository<SysUser, Long> implements UserPort {

    private static final SysUserTable table = SysUserTable.$;

    public UserPortImpl(JSqlClient sql) {
        super(sql);
    }

    @Override
    public AbstractTypedTable<SysUser> getTable() {
        return table;
    }

    @Nullable
    @Override
    @Cacheable(cacheNames = "u", key = "#username", unless = "#result==null")
    public UserDetailsView findByUsername(String username) {
        List<UserDetailsView> list = createQuery().where(table.name()
                        .eq(username))
                .select(table.fetch(UserDetailsView.class))
                .limit(1)
                .execute();
        if (list.isEmpty()) {
            return null;
        }
        return list.getFirst();
    }

    @Override
    public Boolean update(UserUpdateInput input) {
        return sql.saveCommand(input)
                .setOptimisticLock(SysUserTable.class, new CommandDataScopeFilter<>())
                .execute()
                .isModified();
    }

    @Override
    public SysUser register(Input<SysUser> input) {
        return saveCommand(input)
                .setMode(org.babyfish.jimmer.sql.ast.mutation.SaveMode.INSERT_ONLY)
                .execute()
                .getModifiedEntity();
    }

    @Override
    public <V extends View<SysUser>> List<V> select(Class<V> viewClass) {
        return super.select(viewClass);
    }

    @Override
    public List<SysUser> select(Fetcher<SysUser> fetcher) {
        return super.select(fetcher);
    }

    @Override
    public <V extends View<SysUser>> List<V> select(Class<V> viewClass, UserSpecification specification) {
        return createQuery().where(specification)
                .orderBy(getCreatedTable().createdAt()
                        .desc())
                .select(getTable().fetch(viewClass))
                .execute();
    }

    @Override
    public Page<SysUser> select(Fetcher<SysUser> fetcher, PageReq pageReq, UserSpecification specification) {
        return super.select(fetcher, pageReq, specification);
    }
}
