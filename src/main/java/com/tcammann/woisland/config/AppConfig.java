package com.tcammann.woisland.config;

import com.tcammann.woisland.service.EventListener;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import discord4j.core.event.domain.Event;
import java.util.List;

@Configuration
public class AppConfig {
    @Bean
    public String clientToken(
            @Value("${discord.client.token.value}") final String applicationVariable,
            @Value("${discord.client.token.key.environment}") final String environmentVariableKey){
        String environmentVariable = System.getenv(environmentVariableKey);
        return environmentVariable != null ? environmentVariable : applicationVariable;
    }

    @Bean
    public <T extends Event> GatewayDiscordClient gatewayDiscordClient(
            final List<EventListener<T>> eventListeners, final String clientToken) {
        GatewayDiscordClient gatewayDiscordClient = DiscordClientBuilder.create(clientToken)
                .build()
                .login()
                .block();

        for(EventListener<T> listener : eventListeners) {
            assert gatewayDiscordClient != null;
            gatewayDiscordClient.on(listener.getEventType())
                    .flatMap(listener::execute)
                    .onErrorResume(listener::handleError)
                    .subscribe();
        }

        return gatewayDiscordClient;
    }
}
