package org.ninng.businesssvc.interceptor;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.val;
import org.jspecify.annotations.NonNull;
import org.ninng.businesssvc.component.DatabaseUserDetailsService;
import org.ninng.businesssvc.component.I18nUtil;
import org.ninng.businesssvc.component.JwtTokenUtil;
import org.ninng.businesssvc.constant.HttpConstant;
import org.ninng.businesssvc.context.UserContextHolder;
import org.ninng.businesssvc.entity.exception.ServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenUtil jwtTokenUtil;
    private final DatabaseUserDetailsService userDetailsService;
    private final I18nUtil i18n;

    public AuthenticationFilter(JwtTokenUtil jwtTokenUtil, DatabaseUserDetailsService userDetailsService,
                                I18nUtil i18n) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.userDetailsService = userDetailsService;
        this.i18n = i18n;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        val tenantId = request.getHeader(HttpConstant.TENANT_ID);
        if (tenantId == null) {
            throw new ServiceException(i18n.getMessage("exception.noTenantId"));
        }
        UserContextHolder.setTenantId(tenantId.trim());

        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        // 提取 Token
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            try {
                username = jwtTokenUtil.extractUsername(jwt);
            } catch (Exception e) {
                logger.error("Error parsing JWT", e);
            }
        }

        // 验证 Token 并设置 SecurityContext
        if (username != null && SecurityContextHolder.getContext()
                .getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            if (jwtTokenUtil.validateToken(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 设置认证信息到上下文
                SecurityContextHolder.getContext()
                        .setAuthentication(authenticationToken);
            }
        }
        filterChain.doFilter(request, response);
    }
}
