package org.ninng.businesssvc.service;

import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.ninng.businesssvc.model.SysRole;
import org.ninng.businesssvc.model.dto.RoleCreateInput;
import org.ninng.businesssvc.repository.RoleRepository;
import org.springframework.stereotype.Service;

@Service
public class RoleService {

    public final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public SysRole create(Fetcher<SysRole> fetcher, RoleCreateInput input) {
        return roleRepository.saveCommand(input)
                .setMode(SaveMode.INSERT_ONLY)
                .execute(fetcher)
                .getModifiedEntity();
    }
}
