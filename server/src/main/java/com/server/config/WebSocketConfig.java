package com.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS(); // If using SockJS on the client
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.setUserDestinationPrefix("/user");
        config.setApplicationDestinationPrefixes("/app");
        config.enableSimpleBroker("/topic", "/queue");
    }

    @Override
    public void configureWebSocketTransport(org.springframework.web.socket.config.annotation.WebSocketTransportRegistration registration) {
        registration
                .setMessageSizeLimit(10 * 1024 * 1024)        // 10MB max message size
                .setSendBufferSizeLimit(10 * 1024 * 1024)     // 10MB send buffer
                .setSendTimeLimit(30 * 1000);                 // 30 seconds to send messages
    }
}
