package org.ninng.businesssvc.identity.domain.port;

import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.identity.application.dto.RoleUpdateInput;
import org.ninng.businesssvc.identity.domain.model.SysRole;

public interface RolePort {

    SysRole create(Fetcher<SysRole> fetcher, Input<SysRole> input);

    Boolean update(RoleUpdateInput input);
}
