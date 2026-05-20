package org.ninng.businesssvc.constant;

public class CacheConstant {

    public static final String KEY_PREFIX = "onion";
    public static final String USER = "u";
    public static final String USER_ROLE = "ur";

    /**
     * Redis Pub/Sub channel for cross-instance cache invalidation
     */
    public static final String INVALIDATION_CHANNEL = "%s:cache:invalidate".formatted(KEY_PREFIX);
}
