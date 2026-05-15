package org.ninng.businesssvc.service;

import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.ninng.businesssvc.model.SysUser;
import org.ninng.businesssvc.model.dto.RegisterInput;
import org.ninng.businesssvc.model.dto.UserUpdateInput;
import org.ninng.businesssvc.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    public final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public SysUser register(RegisterInput registerInput) {
        return userRepository.save(registerInput, SaveMode.INSERT_ONLY)
                .getModifiedEntity();
    }

    public Boolean update(UserUpdateInput input) {
        return userRepository.update(input);
    }
}
