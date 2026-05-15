package org.ninng.businesssvc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.nio.charset.StandardCharsets;

@Configuration
public class I18nConfig {


    @Bean(name = "messageSource")
    public ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("i18n/messages");
        // 默认编码
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        // 找不到 key 时，返回 key 作为默认值，而不是抛出异常
        messageSource.setUseCodeAsDefaultMessage(true);
        return messageSource;
    }
}
