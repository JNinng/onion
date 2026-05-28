package org.ninng.businesssvc.identity.domain.port;

import jakarta.annotation.Nullable;
import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.View;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.identity.application.dto.UserDetailsView;
import org.ninng.businesssvc.identity.application.dto.UserSpecification;
import org.ninng.businesssvc.identity.application.dto.UserUpdateInput;
import org.ninng.businesssvc.identity.domain.model.SysUser;

import java.util.List;

public interface UserPort {

    @Nullable
    UserDetailsView findByUsername(String username);

    Boolean update(UserUpdateInput input);

    SysUser register(org.babyfish.jimmer.Input<SysUser> input);

    <V extends View<SysUser>> List<V> select(Class<V> viewClass);

    <V extends View<SysUser>> List<V> select(Class<V> viewClass, UserSpecification specification);

    List<SysUser> select(Fetcher<SysUser> fetcher);

    Page<SysUser> select(Fetcher<SysUser> fetcher, PageReq pageReq, UserSpecification specification);
}
