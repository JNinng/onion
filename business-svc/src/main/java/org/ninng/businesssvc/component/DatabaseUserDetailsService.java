package org.ninng.businesssvc.component;

import org.ninng.businesssvc.context.UserContextHolder;
import org.ninng.businesssvc.entity.exception.ServiceException;
import org.ninng.businesssvc.model.dto.UserDetailsView;
import org.ninng.businesssvc.repository.UserRepository;
import org.ninng.businesssvc.repository.UserRoleRepository;
import org.ninng.businesssvc.role.application.dto.RoleDetailsView;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final I18nUtil i18n;

    public DatabaseUserDetailsService(UserRepository userRepository, UserRoleRepository userRoleRepository,
                                      I18nUtil i18n) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.i18n = i18n;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDetailsView user = UserContextHolder.callWithDisabled(() -> userRepository.findByUsername(username));
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
                () -> userRoleRepository.findByUserId(user.getId()));
        if (roleList.isEmpty()) {
            throw new ServiceException(i18n.getMessage("exception.undistributedRole"));
        }
        UserContextHolder.setRoles(roleList);
        return new User(user.getName(), user.getPassword(), roleList.stream()
                .map(roleDetailsView -> new SimpleGrantedAuthority(roleDetailsView.getCode()))
                .toList());
    }
}
