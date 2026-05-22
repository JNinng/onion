package org.ninng.businesssvc.constant;

import java.time.Duration;

public class C {

    public static final String TRACE_ID = "traceId";

    public static final String ORM_AUTO_KEY_PREFIX = CacheConstant.KEY_PREFIX + ":orm:auto:";
    public static final Duration ORM_AUTO_REMOTE_CACHE_DURATION = Duration.ofHours(4);
    public static final int ORM_AUTO_LOCAL_CACHE_SIZE = 1_0000;
    public static final Duration ORM_AUTO_LOCAL_CACHE_DURATION = Duration.ofHours(2);
    public static final int ORM_AUTO_MULTI_VIEW_LOCAL_CACHE_SIZE = 1000;
    public static final Duration ORM_AUTO_MULTI_VIEW_LOCAL_CACHE_DURATION = Duration.ofHours(1);
    public static final Duration ORM_AUTO_MULTI_VIEW_REMOTTE_CACHE_DURATION = Duration.ofHours(2);

    public final static char[] ALPHANUMERIC = new char[]{'1', '2', '3', '4', '5', '6', '7', '8', '9', '0', 'a', 'b',
            'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w',
            'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R',
            'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};
    public final static char[] LOWER_CASE_ALPHANUMERIC = new char[]{'1', '2', '3', '4', '5', '6', '7', '8', '9', '0',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u',
            'v', 'w', 'x', 'y', 'z'};
    /**
     * 移除字母 o, l
     */
    public final static char[] LOWER_CASE_ID_ALPHANUMERIC = new char[]{'1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'm', 'n', 'p', 'q', 'r', 's', 't', 'u', 'v',
            'w', 'x', 'y', 'z'};
    public final static int TRACE_ID_LENGTH = 16;
    public final static int TENANT_ID_LENGTH = 16;
    /**
     * 租户ID 字符集（排除 0/O/1/I 等相似字符）
     */
    public final static char[] TENANT_ID_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();

    public static class Data {
        /**
         * 禁用
         */
        public final static int DISABLED = 0;
        /**
         * 启用
         */
        public final static int ENABLED = 1;
    }
}
