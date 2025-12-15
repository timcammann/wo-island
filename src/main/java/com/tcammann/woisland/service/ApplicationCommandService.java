package com.tcammann.woisland.service;


import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class ApplicationCommandService {
    final GatewayDiscordClient gatewayDiscordClient;


    public ApplicationCommandService(GatewayDiscordClient gatewayDiscordClient) {
        this.gatewayDiscordClient = gatewayDiscordClient;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void register(){
        long applicationId = gatewayDiscordClient.getRestClient().getApplicationId().block();

        ApplicationCommandRequest commandRequest = ApplicationCommandRequest.builder()
                .name("petcounter")
                .description("The official wo-island pet counter")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("period")
                        .description("year, month or week")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(false)
                        .build())
                .build();

        // For guild-specific (recommended for dev): TODO: make configurable / switch to global
        long guildId = 1151945311472861316L;  // Replace with your guild ID
        gatewayDiscordClient.getRestClient().getApplicationService()
                .createGuildApplicationCommand(applicationId, guildId, commandRequest)
                .subscribe();

        System.out.println("Application command registered");

// For global:
//client.getRestClient().getApplicationService()
//    .createGlobalApplicationCommand(applicationId, commandRequest)
//    .subscribe();

    }

}
