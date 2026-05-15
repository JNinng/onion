package org.ninng.businesssvc.entity;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import java.io.Serial;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SecretReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 352296644323431672L;

    @NotBlank
    @Length(min = 390, max = 600)
    private String clientPublicKey;
}
