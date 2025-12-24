package com.tcammann.woisland.config;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DiscordConfig {

    @Bean
    public <T extends Event> GatewayDiscordClient gatewayDiscordClient(@Value("${discord.client.token.value}") final String clientToken) {
        return DiscordClientBuilder.create(clientToken)
                .build()
                .login()
                .block();
    }
}
