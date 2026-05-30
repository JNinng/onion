package org.ninng.businesssvc.version;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.ninng.businesssvc.constant.HttpConstant;
import org.ninng.businesssvc.context.SpringContextHolder;
import org.ninng.businesssvc.entity.exception.ServiceException;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.RequestCondition;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;

/**
 * 扩展 {@link RequestMappingHandlerMapping}，从 {@link ApiVersion} 注解生成版本匹配条件。
 */
public class ApiVersionHandlerMapping extends RequestMappingHandlerMapping {

    @Override
    protected void initHandlerMethods() {
        ApiVersionRequestCondition.clearRegistry();
        super.initHandlerMethods();
    }

    @Override
    protected RequestCondition<?> getCustomTypeCondition(@NonNull Class<?> handlerType) {
        ApiVersion annotation = AnnotationUtils.findAnnotation(handlerType, ApiVersion.class);
        if (annotation != null) {
            return new ApiVersionRequestCondition(annotation.value(), annotation.deprecated(), annotation.sunset());
        }
        return null;
    }

    @Override
    protected RequestCondition<?> getCustomMethodCondition(@NonNull Method method) {
        ApiVersion annotation = AnnotationUtils.findAnnotation(method, ApiVersion.class);
        if (annotation != null) {
            return new ApiVersionRequestCondition(annotation.value(), annotation.deprecated(), annotation.sunset());
        }
        return null;
    }

    /**
     * 当请求携带 X-Api-Version 头但无处理器匹配时，返回 400 而非 404。
     */
    @Override
    protected HandlerMethod lookupHandlerMethod(@NonNull String lookupPath,
                                                @NonNull HttpServletRequest request) throws Exception {
        HandlerMethod handler = super.lookupHandlerMethod(lookupPath, request);
        if (handler == null) {
            String version = request.getHeader(HttpConstant.API_VERSION);
            if (version != null && !version.isBlank()) {
                throw new ServiceException(SpringContextHolder.getBean(org.ninng.businesssvc.component.I18nUtil.class)
                        .getMessage("exception.apiVersionNotFound", new Object[]{version}));
            }
        }
        return handler;
    }
}
