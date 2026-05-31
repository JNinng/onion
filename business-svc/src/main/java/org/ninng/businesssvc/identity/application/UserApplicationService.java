package org.ninng.businesssvc.identity.application;

import lombok.val;
import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.common.domain.port.RoleCheckerPort;
import org.ninng.businesssvc.common.domain.port.UserCheckerPort;
import org.ninng.businesssvc.context.UserContextHolder;
import org.ninng.businesssvc.context.UserContextMode;
import org.ninng.businesssvc.entity.PageReq;
import org.ninng.businesssvc.identity.application.dto.*;
import org.ninng.businesssvc.identity.domain.model.SysUser;
import org.ninng.businesssvc.identity.domain.port.UserPort;
import org.ninng.businesssvc.identity.domain.port.UserRolePort;
import org.ninng.businesssvc.utils.CollectionDiffUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserApplicationService {

    private final UserPort userPort;
    private final UserCheckerPort userCheckerPort;
    private final RoleCheckerPort roleCheckerPort;
    private final UserRolePort userRolePort;

    public UserApplicationService(UserPort userPort, UserCheckerPort userCheckerPort, RoleCheckerPort roleCheckerPort,
                                  UserRolePort userRolePort) {
        this.userPort = userPort;
        this.userCheckerPort = userCheckerPort;
        this.roleCheckerPort = roleCheckerPort;
        this.userRolePort = userRolePort;
    }

    public SysUser register(RegisterInput registerInput) {
        return userPort.register(registerInput);
    }

    public Boolean update(UserUpdateInput input) {
        if (input.getId() == null) {
            return false;
        }
        userCheckerPort.checkVisible(input.getId());
        val roleIds = input.getRoleIds();
        if (!roleIds.isEmpty()) {
            val oldRoleIds = UserContextHolder.withMode(UserContextMode.DisabledType.INSTANCE,
                            () -> userRolePort.findByUserId(input.getId()))
                    .stream()
                    .map(RoleDetailsView::getId)
                    .toList();
            val diff = CollectionDiffUtils.diff(oldRoleIds, roleIds);
            roleCheckerPort.checkVisible(diff.changed());
        }
        return userPort.update(input);
    }

    public Page<SysUser> list(Fetcher<SysUser> fetcher, PageReq pageReq, UserSpecification specification) {
        return userPort.select(fetcher, pageReq, specification);
    }

    public List<UserSelectionView> selections() {
        return userPort.select(UserSelectionView.class);
    }
}
