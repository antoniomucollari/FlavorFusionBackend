package com.toni.FoodApp.config;

import com.toni.FoodApp.security.JwtUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${spring.rabbitmq.password}")
    private String pass;

    @Value("${spring.rabbitmq.host}")
    private String rabbitmqIp;

    @Value("${spring.rabbitmq.username}")
    private String username;


    private final JwtUtils jwtUtils;

    public WebSocketConfig(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }


    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableStompBrokerRelay("/topic", "/queue")
                .setRelayHost(rabbitmqIp)
                .setRelayPort(61613)
                .setClientLogin(username)
                .setClientPasscode(pass)
                .setSystemLogin(username)
                .setSystemPasscode(pass);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                // Only intercept the initial CONNECT command
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {

                    // Grab the Authorization header
                    String authHeader = accessor.getFirstNativeHeader("Authorization");

                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);

                        try {
                            // TODO: Extract the email from your token using your JwtService
                             String userEmail = jwtUtils.getUsernameFromToken(token);
                            if (userEmail != null) {
                                Principal userPrincipal = new Principal() {
                                    @Override
                                    public String getName() {
                                        return userEmail;
                                    }
                                };

                                accessor.setUser(userPrincipal);
                            }
                        } catch (Exception e) {
                            // Token is invalid/expired
                            System.out.println("WebSocket JWT Error: " + e.getMessage());
                        }
                    }
                }
                return message;
            }
        });
    }
}