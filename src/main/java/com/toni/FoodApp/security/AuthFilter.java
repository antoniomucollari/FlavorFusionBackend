package com.toni.FoodApp.security;

import com.toni.FoodApp.exceptions.CustomAuthenticationEntryPoint;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.internal.util.stereotypes.Lazy;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthFilter  extends OncePerRequestFilter {
    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService customUserDetailsService;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private static final List<String> PASSWORD_CHANGE_WHITELIST = List.of(
            "/api/users/change-password"
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
             HttpServletResponse response,
             FilterChain filterChain
    ) throws ServletException, IOException {

        String token = getTokenFromRequest(request);
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        if (token != null){
            String email;
            try {
                email = jwtUtils.getUsernameFromToken(token);
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

                if (StringUtils.hasText(email) && jwtUtils.isTokenValid(token, userDetails)) {

                    // EXTRACT VERSION FROM JWT
                    Integer jwtVersion = jwtUtils.getTokenVersionFromToken(token);

                    if (userDetails instanceof CustomUserDetails customUser) {

                        // get from db
                        Integer dbVersion = customUser.getTokenVersion();

                        if (jwtVersion == null || !jwtVersion.equals(dbVersion)) {
                            // Token is stale.
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\": \"Session expired. Please log in again.\"}");
                            return; // STOP execution here! Do not authenticate.
                        }

                        //Check if account is deactivated
                        if (!customUser.isEnabled()) {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\": \"User account is deactivated.\"}");
                            return;
                        }

                        // Existing logic: Check if password change is required
                        if (!customUser.isCredentialsNonExpired()) {
                            String requestPath = request.getServletPath();

                            boolean isAllowed = PASSWORD_CHANGE_WHITELIST.stream()
                                    .anyMatch(path -> requestPath.equals(path));

                            if (!isAllowed) {
                                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                response.setContentType("application/json");
                                response.getWriter().write("{\"error\": \"Password change required.\", \"code\": \"PASSWORD_CHANGE_REQUIRED\"}");
                                return;
                            }
                        }
                    }

                    // If all checks pass, authenticate the user!
                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            } catch (Exception e) {
                AuthenticationException authenticationException = new BadCredentialsException(e.getMessage());
                customAuthenticationEntryPoint.commence(request, response, authenticationException);
                return;
            }
        }
        try {
            filterChain.doFilter(request, response);
        }
        catch (Exception e){
            log.error("Error in filter chain: ", e);
        }
    }
    private String getTokenFromRequest(HttpServletRequest request){
        String bearerToken = request.getHeader("Authorization");
        if(bearerToken != null && bearerToken.startsWith("Bearer ")){
            return bearerToken.substring(7);
        }
        return null;

    }

}
