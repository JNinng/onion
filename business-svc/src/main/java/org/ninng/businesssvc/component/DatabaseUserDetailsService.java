package org.ninng.businesssvc.component;

import org.ninng.businesssvc.context.UserContextHolder;
import org.ninng.businesssvc.entity.exception.ServiceException;
import org.ninng.businesssvc.identity.application.dto.RoleDetailsView;
import org.ninng.businesssvc.identity.application.dto.UserDetailsView;
import org.ninng.businesssvc.identity.domain.port.UserPort;
import org.ninng.businesssvc.identity.domain.port.UserRolePort;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserPort userPort;
    private final UserRolePort userRolePort;
    private final I18nUtil i18n;

    public DatabaseUserDetailsService(UserPort userPort, UserRolePort userRolePort,
                                      I18nUtil i18n) {
        this.userPort = userPort;
        this.userRolePort = userRolePort;
        this.i18n = i18n;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDetailsView user = UserContextHolder.callWithDisabled(() -> userPort.findByUsername(username));
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }

        var tenantId = user.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(i18n.getMessage("exception.noTenantId"));
        }
        UserContextHolder.setTenantId(tenantId);

        UserContextHolder.setUser(user);
        var ownerDeptId = user.getOwnerDeptId();
        if (ownerDeptId == null) {
            throw new ServiceException(i18n.getMessage("exception.noDept"));
        }
        UserContextHolder.setDeptId(ownerDeptId);

        List<RoleDetailsView> roleList = UserContextHolder.callWithDisabled(
                () -> userRolePort.findByUserId(user.getId()));
        if (roleList.isEmpty()) {
            throw new ServiceException(i18n.getMessage("exception.undistributedRole"));
        }
        UserContextHolder.setRoles(roleList);
        return new User(user.getName(), user.getPassword(), roleList.stream()
                .map(roleDetailsView -> new SimpleGrantedAuthority(roleDetailsView.getCode()))
                .toList());
    }
}
