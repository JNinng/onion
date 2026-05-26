package org.ninng.businesssvc.repository;

import jakarta.annotation.Nullable;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.table.spi.AbstractTypedTable;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.model.SysUser;
import org.ninng.businesssvc.model.SysUserFetcher;
import org.ninng.businesssvc.model.SysUserTable;
import org.ninng.businesssvc.model.dto.UserDetailsView;
import org.ninng.businesssvc.model.dto.UserUpdateInput;
import org.ninng.businesssvc.model.filter.CommandDataScopeFilter;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserRepository extends CommonRepository<SysUser, Long> {

    private static final SysUserTable table = SysUserTable.$;

    private static final Fetcher<SysUser> DEFAULT_FETCHER = SysUserFetcher.$.allScalarFields();

    public UserRepository(JSqlClient sql) {
        super(sql);
    }

    @Nullable
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

    @Nullable
    public SysUser user() {
        List<SysUser> list = createQuery().select(table.fetch(DEFAULT_FETCHER))
                .limit(1)
                .execute();
        if (list.isEmpty()) {
            return null;
        }
        return list.getFirst();
    }

    @Override
    public AbstractTypedTable<SysUser> getTable() {
        return table;
    }

    /**
     * <pre>
     *      MutableUpdate update = sql.createUpdate(table);
     *      if (input.getNickname() != null) {
     *          update.set(table.nickname(), input.getNickname());
     *      }
     *      return update.where(table.id()
     *          .eq(input.getId()))
     *          .execute() > 0;
     * </pre>
     */
    public Boolean update(UserUpdateInput input) {
        return sql.saveCommand(input)
                .setOptimisticLock(SysUserTable.class, new CommandDataScopeFilter<>())
                .execute()
                .isModified();
    }
}
