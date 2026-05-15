package org.ninng.businesssvc.context;

import com.alibaba.ttl.TransmittableThreadLocal;
import jakarta.annotation.Nullable;

/**
 * 链路上下文
 */
public class LinkContextHolder {

    private static final TransmittableThreadLocal<String> traceId = new TransmittableThreadLocal<>();

    public static void removes() {
        traceId.remove();
    }

    @Nullable
    public static String getTraceId() {
        return traceId.get();
    }

    public static void setTraceId(@Nullable String traceId) {
        LinkContextHolder.traceId.set(traceId);
    }
}
