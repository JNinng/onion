package org.ninng.businesssvc.context;

import com.alibaba.ttl.TransmittableThreadLocal;
import jakarta.annotation.Nullable;

public class SecurityContextHolder {

    private static final TransmittableThreadLocal<String> acceptAlgorithm = new TransmittableThreadLocal<>();

    public static void removes() {
        acceptAlgorithm.remove();
    }

    @Nullable
    public static String getAcceptAlgorithm() {
        return acceptAlgorithm.get();
    }

    public static void setAcceptAlgorithm(@Nullable String acceptAlgorithm) {
        SecurityContextHolder.acceptAlgorithm.set(acceptAlgorithm);
    }
}
