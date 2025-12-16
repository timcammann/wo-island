package com.tcammann.woisland.service;


import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InitService<T extends Event> {
    Logger LOG = LoggerFactory.getLogger(InitService.class);
    final GatewayDiscordClient gatewayDiscordClient;
    final List<Listener<T>> listeners;


    public InitService(GatewayDiscordClient gatewayDiscordClient, final List<Listener<T>> listeners) {
        this.gatewayDiscordClient = gatewayDiscordClient;
        this.listeners = listeners;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void register(){

        for(var listener : listeners) {
            gatewayDiscordClient.on(listener.getEventType())
                    .flatMap(listener::execute)
                    .onErrorContinue(listener::handleError)
                    .doOnComplete(() -> LOG.warn("{} terminated!", listener.getClass().getSimpleName()))
                    .subscribe();

            LOG.info("{} listening to {}", listener.getClass().getSimpleName(), listener.getEventType().getSimpleName());
        }

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
                .doOnSuccess(command -> LOG.info("Application command '{}' registered", command.name()))
                .subscribe();

        // For global:
        //client.getRestClient().getApplicationService()
        //    .createGlobalApplicationCommand(applicationId, commandRequest)
        //    .subscribe();

    }

}
