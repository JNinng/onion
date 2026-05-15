package org.ninng.businesssvc.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InfoResp implements Serializable {

    @Serial
    private static final long serialVersionUID = -2954495226952274834L;

    /**
     * 支持的加密算法
     */
    private List<String> algorithms;
}
