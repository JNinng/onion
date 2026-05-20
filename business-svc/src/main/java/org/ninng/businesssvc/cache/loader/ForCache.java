package org.ninng.businesssvc.cache.loader;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ForCache {
    String value();
}
