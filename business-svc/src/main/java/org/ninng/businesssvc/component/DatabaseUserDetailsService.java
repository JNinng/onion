package org.ninng.businesssvc.component;

import org.ninng.businesssvc.context.UserContextHolder;
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

    public DatabaseUserDetailsService(UserRepository userRepository, UserRoleRepository userRoleRepository) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDetailsView user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        UserContextHolder.setTenantId(user.getTenantId());
        UserContextHolder.setUser(user);
        List<RoleDetailsView> roleList = userRoleRepository.findByUserId(user.getId());
        UserContextHolder.setRoles(roleList);
        return new User(user.getName(), user.getPassword(), roleList.stream()
                .map(roleDetailsView -> new SimpleGrantedAuthority(roleDetailsView.getCode()))
                .toList());
    }
}
