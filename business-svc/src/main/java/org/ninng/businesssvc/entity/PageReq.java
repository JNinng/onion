package org.ninng.businesssvc.entity;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageReq implements Serializable {

    @Serial
    private static final long serialVersionUID = -1964760242267180665L;

    @Min(0)
    private int pageIndex = 0;
    @Max(2000)
    private int pageSize = 20;
}