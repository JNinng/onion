package org.ninng.businesssvc.version;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 读取匹配到的处理器上的 {@link ApiVersion} 注解，设置废弃/下线响应头。
 */
public class ApiVersionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        if (handler instanceof HandlerMethod hm) {
            ApiVersion ann = hm.getMethodAnnotation(ApiVersion.class);
            if (ann == null) {
                ann = AnnotationUtils.findAnnotation(hm.getBeanType(), ApiVersion.class);
            }
            if (ann != null && ann.deprecated()) {
                response.setHeader("Deprecation", "true");
                if (!ann.sunset()
                        .isEmpty()) {
                    response.setHeader("Sunset", ann.sunset());
                }
            }
        }
        return true;
    }
}
