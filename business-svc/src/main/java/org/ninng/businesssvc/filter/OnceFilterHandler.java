package org.ninng.businesssvc.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;

public interface OnceFilterHandler {

    void before(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response);

    void after(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response);
}
