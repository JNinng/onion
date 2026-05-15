package org.ninng.businesssvc.component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;

@Component("localeResolver")
public class RequestLocaleResolver implements LocaleResolver {

    @Override
    public @NonNull Locale resolveLocale(HttpServletRequest request) {
        // 优先 URL 参数 ?lang=zh_CN
        String lang = request.getParameter("lang");
        if (lang != null && !lang.isEmpty()) {
            return Locale.forLanguageTag(lang.replace("_", "-"));
        }

        // 其次 Header (Accept-Language)
        String headerLang = request.getHeader("Accept-Language");
        if (headerLang != null && !headerLang.isEmpty()) {
            return request.getLocale();
        }

        // 默认中文
        return Locale.SIMPLIFIED_CHINESE;
    }

    @Override
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
    }
}
