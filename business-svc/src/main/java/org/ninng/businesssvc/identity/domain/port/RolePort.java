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

public interface RolePort {

    SysRole create(Fetcher<SysRole> fetcher, Input<SysRole> input);

    Boolean update(RoleUpdateInput input);

    long countVisible(@NonNull List<Long> changeRoleIds);

    Page<SysRole> select(Fetcher<SysRole> fetcher, PageReq pageReq, RoleSpecification specification);
}
