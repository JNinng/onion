package org.ninng.businesssvc.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 2397974827303347252L;

    private String token;
}
