package com.toni.FoodApp.exceptions;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.toni.FoodApp.response.Response;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
//WHAT IS ---> a user who is already logged in (authenticated) tries to access
//a resource they don't have permission for (unauthorized).
@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {
    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        Response<?> errorResponse = Response.builder()
                .statusCode(HttpStatus.FORBIDDEN.value()) //403
                .message(accessDeniedException.getMessage()).build();

        // Set response details
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Write JSON body
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
