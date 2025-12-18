package com.tcammann.woisland.config;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    @Bean
    public String clientToken(
            @Value("${discord.client.token.value}") final String applicationVariable,
            @Value("${discord.client.token.key.environment}") final String environmentVariableKey) {
        String environmentVariable = System.getenv(environmentVariableKey); // TODO: Unnecessary, refactor
        return environmentVariable != null ? environmentVariable : applicationVariable;
    }

    @Bean
    public <T extends Event> GatewayDiscordClient gatewayDiscordClient(final String clientToken) {
        return DiscordClientBuilder.create(clientToken)
                .build()
                .login()
                .block();
    }
}
