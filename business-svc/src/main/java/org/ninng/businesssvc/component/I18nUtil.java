package org.ninng.businesssvc.component;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class I18nUtil {

    private final MessageSource messageSource;

    public I18nUtil(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * 根据 key 获取当前语言环境下的消息
     *
     * @param key 消息键
     * @return 翻译后的文本
     */
    public String getMessage(String key) {
        return getMessage(key, null);
    }

    /**
     * 带参数的消息获取
     *
     * @param key  消息键
     * @param args 参数数组
     * @return 翻译后的文本
     */
    public String getMessage(String key, Object[] args) {
        // 默认会解析 HTTP Header 中的 Accept-Language
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(key, args, locale);
    }
}