package org.ninng.businesssvc.identity.application;

import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.identity.application.dto.RoleCreateInput;
import org.ninng.businesssvc.identity.application.dto.RoleUpdateInput;
import org.ninng.businesssvc.identity.domain.model.SysRole;
import org.ninng.businesssvc.identity.domain.port.RolePort;
import org.springframework.stereotype.Service;

@Service
public class RoleApplicationService {

    private final RolePort roleRepository;

    public RoleApplicationService(RolePort roleRepository) {
        this.roleRepository = roleRepository;
    }

    public SysRole create(Fetcher<SysRole> fetcher, RoleCreateInput input) {
        return roleRepository.create(fetcher, input);
    }

    public Boolean update(RoleUpdateInput input) {
        var roleIdScopes = input.getRoleIdScopes();
        if (roleIdScopes != null) {
            roleIdScopes.forEach(scope -> scope.setRoleId(input.getId()));
        }
        input.setRoleIdScopes(null);
        return roleRepository.update(input);
    }
}
