package org.ninng.businesssvc.identity.service;

import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.identity.application.dto.RegisterInput;
import org.ninng.businesssvc.identity.application.dto.UserUpdateInput;
import org.ninng.businesssvc.identity.domain.model.SysUser;
import org.ninng.businesssvc.identity.domain.port.UserPort;
import org.springframework.stereotype.Service;

@Service
public class UserApplicationService {

    private final UserPort userPort;

    public UserApplicationService(UserPort userPort) {
        this.userPort = userPort;
    }

    public SysUser register(RegisterInput registerInput) {
        return userPort.register((Input<SysUser>) registerInput);
    }

    public Boolean update(UserUpdateInput input) {
        return userPort.update(input);
    }
}
