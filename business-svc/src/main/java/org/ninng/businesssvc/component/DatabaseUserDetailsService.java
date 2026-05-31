package org.ninng.businesssvc.component;

import org.ninng.businesssvc.common.domain.port.UserQueryPort;
import org.ninng.businesssvc.context.UserContextHolder;
import org.ninng.businesssvc.entity.exception.ServiceException;
import org.ninng.businesssvc.entity.exception.UserNotFoundException;
import org.ninng.businesssvc.identity.application.dto.RoleDetailsView;
import org.ninng.businesssvc.identity.application.dto.UserDetailsView;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserQueryPort userQueryPort;
    private final I18nUtil i18n;

    public DatabaseUserDetailsService(UserQueryPort userQueryPort, I18nUtil i18n) {
        this.userQueryPort = userQueryPort;
        this.i18n = i18n;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDetailsView user;
        try {
            user = UserContextHolder.withOwnerDisabled(() -> userQueryPort.userDetailsByUsername(username));
        } catch (UserNotFoundException e) {
            throw new UsernameNotFoundException(username);
        }
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        UserContextHolder.setUser(user);
        var ownerDeptId = user.getOwnerDeptId();
        if (ownerDeptId == null) {
            throw new ServiceException(i18n.getMessage("exception.noDept"));
        }
        UserContextHolder.setDeptId(ownerDeptId);

        var tenantId = user.getTenantId();
        if (tenantId == null) {
            throw new ServiceException(i18n.getMessage("exception.noTenantId"));
        }
        UserContextHolder.setTenantId(tenantId);

        List<RoleDetailsView> roleList = UserContextHolder.withOwnerDisabled(
                () -> userQueryPort.roleDetailsByUserId(user.getId()));
        if (roleList.isEmpty()) {
            throw new ServiceException(i18n.getMessage("exception.undistributedRole"));
        }
        UserContextHolder.setRoles(roleList);
        return new User(user.getName(), user.getPassword(), roleList.stream()
                .map(roleDetailsView -> new SimpleGrantedAuthority(roleDetailsView.getCode()))
                .toList());
    }
}
