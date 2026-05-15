package org.ninng.businesssvc.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ninng.businesssvc.constant.HttpConstant;

import java.io.Serial;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class R<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 511653056347900969L;

    private int code;
    private String msg;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String algorithm;

    public static <T> R<T> ok(T data) {
        return new R<>(HttpConstant.SUCCESS, "", data, null);
    }

    public static <T> R<T> fail(String msg) {
        return new R<>(HttpConstant.FAIL, msg, null, null);
    }

    public static <T> R<T> err(String msg) {
        return new R<>(HttpConstant.ERROR, msg, null, null);
    }
}
