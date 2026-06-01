package org.ninng.businesssvc.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ninng.businesssvc.context.LinkContextHolder;
import org.ninng.businesssvc.entity.exception.ErrCode;

import java.io.Serial;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class R<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 8909055382723401771L;

    private String code;
    private String msg;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String algorithm;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String traceId;

    public R(ErrCode code, String msg, T data, String algorithm) {
        this.code = code.getCode();
        this.msg = msg;
        this.data = data;
        this.algorithm = algorithm;
    }

    public static <T> R<T> ok(T data) {
        return new R<>(ErrCode.OK, "", data, null);
    }

    public static <T> R<T> fail(String msg, ErrCode code) {
        return new R<>(code.getCode(), msg, null, null, LinkContextHolder.getTraceId());
    }

    public static <T> R<T> err(String msg, ErrCode code) {
        return new R<>(code.getCode(), msg, null, null, LinkContextHolder.getTraceId());
    }

    public static <T> R<T> err(String msg, ErrCode code, String algorithm) {
        return new R<>(code.getCode(), msg, null, algorithm, LinkContextHolder.getTraceId());
    }
}
