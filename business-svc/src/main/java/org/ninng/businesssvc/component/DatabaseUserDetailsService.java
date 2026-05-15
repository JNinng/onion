package org.ninng.businesssvc.component;

import org.ninng.businesssvc.context.UserContextHolder;
import org.ninng.businesssvc.model.SysUser;
import org.ninng.businesssvc.repository.UserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public DatabaseUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        UserContextHolder.setTenantId(user.tenantId());
        UserContextHolder.setUser(user);
        return new User(user.name(), user.password(), List.of());
    }
}
