package org.ninng.businesssvc.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.RandomStringUtils;
import org.jspecify.annotations.NonNull;
import org.ninng.businesssvc.constant.C;
import org.ninng.businesssvc.constant.HttpConstant;
import org.ninng.businesssvc.context.LinkContextHolder;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class RequestIdOnceFilterHandler implements OnceFilterHandler {

    @Override
    public void before(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response) {
        String requestId = request.getHeader(HttpConstant.REQUEST_ID);
        String traceId = RandomStringUtils.insecure()
                .next(C.TRACE_ID_LENGTH, C.LOWER_CASE_ID_ALPHANUMERIC);

        response.setHeader(HttpConstant.REQUEST_ID, requestId);
        response.setHeader(HttpConstant.RESPONSE_TRACE_ID, traceId);

        LinkContextHolder.setTraceId(traceId);
        MDC.put(C.TRACE_ID, traceId);
    }

    @Override
    public void after(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response) {
        LinkContextHolder.removes();
        MDC.remove(C.TRACE_ID);
    }
}
