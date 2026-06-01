package org.ninng.businesssvc.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class MainOnceFilterHandler extends OncePerRequestFilter {

    private final List<OnceFilterHandler> onceFilterHandlers;

    public MainOnceFilterHandler(List<OnceFilterHandler> onceFilterHandlers) {
        this.onceFilterHandlers = onceFilterHandlers;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        for (OnceFilterHandler onceFilterHandler : onceFilterHandlers) {
            try {
                onceFilterHandler.before(wrappedRequest, response);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        try {
            filterChain.doFilter(wrappedRequest, response);
        } finally {
            for (OnceFilterHandler onceFilterHandler : onceFilterHandlers) {
                try {
                    onceFilterHandler.after(wrappedRequest, response);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }
}
