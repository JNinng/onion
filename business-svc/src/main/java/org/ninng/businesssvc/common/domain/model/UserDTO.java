package org.ninng.businesssvc.common.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.NonNull;
import org.ninng.businesssvc.identity.application.dto.RoleDetailsView;
import org.ninng.businesssvc.identity.application.dto.UserDetailsView;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 2142963592098144480L;

    private UserDetailsView userDetails;
    @NonNull
    private List<RoleDetailsView> roles = List.of();
}
