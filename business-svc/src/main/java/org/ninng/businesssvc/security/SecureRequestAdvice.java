package org.ninng.businesssvc.security;

import com.fasterxml.jackson.core.JsonParseException;
import org.jspecify.annotations.NonNull;
import org.ninng.businesssvc.component.I18nUtil;
import org.ninng.businesssvc.context.SecurityContextHolder;
import org.ninng.businesssvc.entity.exception.SecurityException;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

@ControllerAdvice
public class SecureRequestAdvice extends RequestBodyAdviceAdapter {

    private final AlgorithmHandlerFactory algorithmHandlerFactory;
    private final I18nUtil i18nUtil;

    public SecureRequestAdvice(AlgorithmHandlerFactory algorithmHandlerFactory, I18nUtil i18nUtil) {
        this.algorithmHandlerFactory = algorithmHandlerFactory;
        this.i18nUtil = i18nUtil;
    }

    /**
     * 请求体读取前解密处理
     */
    @Override
    public @NonNull HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, @NonNull MethodParameter parameter,
                                                    @NonNull Type targetType,
                                                    @NonNull Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        // 读取原始请求体
        byte[] originalBody = readInputStreamToBytes(inputMessage.getBody());
        if (originalBody.length == 0) {
            return inputMessage;
        }

        // 解密处理
        byte[] decryptedBody;
        try {
            String originalStr = new String(originalBody, StandardCharsets.UTF_8);
            String decryptedStr = algorithmHandlerFactory.getAlgorithm(SecurityContextHolder.getAcceptAlgorithm())
                    .decrypt(originalStr);
            decryptedBody = decryptedStr.getBytes(StandardCharsets.UTF_8);
        } catch (JsonParseException | NumberFormatException e) {
            throw new SecurityException(i18nUtil.getMessage("security.requestEncryptErr"), e,
                    SecurityContextHolder.getAcceptAlgorithm());
        } catch (Exception e) {
            throw new SecurityException(e.getMessage(), e, SecurityContextHolder.getAcceptAlgorithm());
        }

        // 返回包装后的输入流
        return new DecryptedHttpInputMessage(inputMessage, decryptedBody);
    }

    /**
     * 判断是否需要处理（仅当加解密开关开启，且符合业务范围时返回 true）
     */
    @Override
    public boolean supports(@NonNull MethodParameter methodParameter, @NonNull Type targetType,
                            @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        Security security = methodParameter.getMethodAnnotation(Security.class);
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

    // 读取输入流为字节数组
    private byte[] readInputStreamToBytes(InputStream inputStream) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            return outputStream.toByteArray();
        }
    }

    // 包装解密后的HttpInputMessage
    private record DecryptedHttpInputMessage(HttpInputMessage originalMessage,
                                             ByteArrayInputStream decryptedInputStream) implements HttpInputMessage {
        private DecryptedHttpInputMessage(HttpInputMessage originalMessage, byte[] decryptedInputStream) {
            this(originalMessage, new ByteArrayInputStream(decryptedInputStream));
        }

        @Override
        public @NonNull InputStream getBody() {
            return decryptedInputStream;
        }

        @Override
        public @NonNull HttpHeaders getHeaders() {
            return originalMessage.getHeaders();
        }
    }
}
