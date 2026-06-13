package com.toni.FoodApp.exceptions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toni.FoodApp.response.Response;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authenticationException) throws IOException, ServletException{
        // Check if response has already been committed
        if (response.isCommitted()) {
            return;
        }

        Response<?> errorResponse = Response.builder()
                .statusCode(HttpStatus.UNAUTHORIZED.value()) //401
                .message(authenticationException.getMessage()).build();

        // Set response details
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Write JSON body
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));

//        the result is this:
//        {
//            "statusCode": 401,
//                "message": "Full authentication is required to access this resource"
//        }
    }
}
