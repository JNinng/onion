package org.ninng.businesssvc.version;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记 API 版本。可标注在 Controller 类或方法上，方法级优先级高于类级。
 *
 * <pre>
 * // 精确版本：仅匹配 API-Version: 1.5
 * &#64;ApiVersion("1.5")
 *
 * // 最低版本：匹配 API-Version: 1.2+
 * &#64;ApiVersion("1.2+")
 * </pre>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiVersion {

    /**
     * 版本号，格式 "MAJOR.MINOR" 或 "MAJOR.MINOR+"（最低版本）
     */
    String value() default "1.0+";

    /**
     * 此版本是否已废弃
     */
    boolean deprecated() default false;

    /**
     * 废弃版本的计划下线日期（ISO 格式，如 "2026-12-31"）
     */
    String sunset() default "";
}
