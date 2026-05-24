package org.ninng.businesssvc.role.domain.port;

import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.role.application.dto.RoleUpdateInput;
import org.ninng.businesssvc.role.domain.model.SysRole;

public interface RolePort {

    SysRole create(Fetcher<SysRole> fetcher, Input<SysRole> input);

    Boolean update(RoleUpdateInput input);
}
