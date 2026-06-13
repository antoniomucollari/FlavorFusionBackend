package com.toni.FoodApp.security;

import com.toni.FoodApp.exceptions.CustomAccessDeniedHandler;
import com.toni.FoodApp.exceptions.CustomAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityFilter {

    private final AuthFilter authFilter;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                // 1. Disable CSRF (standard for stateless JWT APIs)
                .csrf(AbstractHttpConfigurer::disable)

                // 2. Enable CORS with our custom configuration source defined below
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 3. Handle Exceptions (401 and 403)
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler(customAccessDeniedHandler)
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                )

                // 4. Define URL permissions
                .authorizeHttpRequests(req -> req
                        // CRITICAL FIX: Allow all OPTIONS requests (Preflight)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Allow public endpoints
                        .requestMatchers( //this returns 401 if is not in this list
                                "/health",
                                "/api/auth/**",
                                "/api/categories/**",
                                "/api/upload/**",
                                "/api/menu/**",
                                "/api/reviews/**",
                                "api/restaurants/available-restaurants/**",
                                "/api/restaurant-branch/**",
                                "/api/roles/**",
                                "/api/restaurant-categories/**",
                                "/api/v1/payments/webhook/**",
                                "/ws/**"
                        ).permitAll()

                        // All other requests must be authenticated
                        .anyRequest().authenticated()
                )

                // 5. Stateless Session (No JSESSIONID)
                .sessionManagement(mag -> mag.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 6. Add your Custom Auth Filter
                .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class);

        return httpSecurity.build();
    }

    /**
     * This Bean defines the CORS rules.
     * It allows your frontend to send requests (including PATCH and OPTIONS).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();


        configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:8080"));

        // ALLOWED METHODS: Important to include PATCH and OPTIONS
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // ALLOWED HEADERS: Authorization is crucial for JWT
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept"));

        // Allow credentials (cookies/auth headers)
        configuration.setAllowCredentials(true);

        // Expose headers if needed (optional)
        configuration.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}