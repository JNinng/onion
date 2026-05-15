package org.ninng.businesssvc.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.ninng.businesssvc.context.UserContextHolder;
import org.springframework.stereotype.Component;

@Component
public class UserContextOnceFilterHandler implements OnceFilterHandler {

    @Override
    public void before(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response) {

    }

    @Override
    public void after(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response) {
        UserContextHolder.removes();
    }
}
