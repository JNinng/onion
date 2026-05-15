package org.ninng.businesssvc.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.ninng.businesssvc.config.SecurityParamConfig;
import org.ninng.businesssvc.constant.HttpConstant;
import org.ninng.businesssvc.context.SecurityContextHolder;
import org.ninng.businesssvc.security.Algorithm;
import org.springframework.stereotype.Component;

@Component
public class SecurityContextOnceFilterHandler implements OnceFilterHandler {

    private final SecurityParamConfig securityParamConfig;

    public SecurityContextOnceFilterHandler(SecurityParamConfig securityParamConfig) {
        this.securityParamConfig = securityParamConfig;
    }

    @Override
    public void before(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response) {
        String acceptAlgorithm = request.getHeader(HttpConstant.ACCEPT_ALGORITHM);
        if (acceptAlgorithm == null || Algorithm.NONE.getValue()
                .equals(acceptAlgorithm)) {
            acceptAlgorithm = securityParamConfig.getDefaultAlgorithm();
        }
        SecurityContextHolder.setAcceptAlgorithm(acceptAlgorithm);
    }

    @Override
    public void after(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response) {
        SecurityContextHolder.removes();
    }
}
