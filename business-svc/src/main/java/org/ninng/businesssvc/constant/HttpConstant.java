package org.ninng.businesssvc.constant;

public class HttpConstant {

    public static final int SUCCESS = 200;
    public static final int FAIL = 500;
    public static final int ERROR = 400;
    public static final String REQUEST_ID = "X-Request-ID";
    public static final String RESPONSE_TRACE_ID = "X-Trace-ID";
    /**
     * 请求头 期望传输加密算法
     */
    public static final String EXPECT_ALGORITHM = "X-Expect-Algorithm";

    /**
     * 请求头 API 版本号
     */
    public static final String API_VERSION = "X-Api-Version";
}
