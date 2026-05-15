package org.ninng.businesssvc.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.ninng.businesssvc.component.I18nUtil;
import org.ninng.businesssvc.config.SecurityParamConfig;
import org.ninng.businesssvc.context.SecurityContextHolder;
import org.ninng.businesssvc.entity.R;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.Map;

@Slf4j
@ControllerAdvice
public class SecureResponseAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;
    private final AlgorithmHandlerFactory algorithmHandlerFactory;
    private final SecurityParamConfig securityParamConfig;
    private final I18nUtil i18nUtil;

    public SecureResponseAdvice(ObjectMapper objectMapper, AlgorithmHandlerFactory algorithmHandlerFactory,
                                SecurityParamConfig securityParamConfig, I18nUtil i18nUtil) {
        this.objectMapper = objectMapper;
        this.algorithmHandlerFactory = algorithmHandlerFactory;
        this.securityParamConfig = securityParamConfig;
        this.i18nUtil = i18nUtil;
    }

    public Object responseBodyHandler(Object body, ObjectMapper objectMapper) {
        try {
            switch (body) {
                case null -> {
                    return null;
                }
                case String plaintext -> {
                    return algorithmHandlerFactory.getAlgorithm(SecurityContextHolder.getAcceptAlgorithm())
                            .encrypt(plaintext);
                }
                case Map<?, ?> rawMap -> {
                    final Map<Object, Object> map = (Map<Object, Object>) rawMap;
                    final String[] targetKeys = map.keySet()
                            .stream()
                            .filter(key -> key instanceof String)
                            .filter(key -> !securityParamConfig.getResponseWhitelist()
                                    .contains(key))
                            .toArray(String[]::new);
                    for (String key : targetKeys) {
                        Object value = map.get(key);
                        if (value == null) {
                            continue;
                        }

                        final String plaintext = objectMapper.writeValueAsString(value);
                        final String encryptedValue =
                                algorithmHandlerFactory.getAlgorithm(SecurityContextHolder.getAcceptAlgorithm())
                                        .encrypt(plaintext);
                        map.put(key, encryptedValue);
                    }

                    return map;
                }
                case R<?> rawR -> {
                    R<Object> r = (R<Object>) rawR;
                    if (r.getData() == null) {
                        return r;
                    }

                    final String plaintext = objectMapper.writeValueAsString(r.getData());
                    r.setData(algorithmHandlerFactory.getAlgorithm(SecurityContextHolder.getAcceptAlgorithm())
                            .encrypt(plaintext));
                    return r;
                }
                default -> {
                    final String plaintext = objectMapper.writeValueAsString(body);
                    return algorithmHandlerFactory.getAlgorithm(SecurityContextHolder.getAcceptAlgorithm())
                            .encrypt(plaintext);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.fail(i18nUtil.getMessage("security.responseErr"));
        }
    }

    @Override
    public Object beforeBodyWrite(Object body, @NonNull MethodParameter returnType,
                                  @NonNull MediaType selectedContentType,
                                  @NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  @NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response) {
        return responseBodyHandler(body, objectMapper);
    }

    @Override
    public boolean supports(@NonNull MethodParameter returnType,
                            @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        Security security = returnType.getMethodAnnotation(Security.class);
        if (security != null) {
            return security.enabled();
        }

        security = converterType.getAnnotation(Security.class);
        if (security != null) {
            return security.enabled();
        }

        return !Algorithm.NONE.getValue()
                .equals(SecurityContextHolder.getAcceptAlgorithm());
    }
}