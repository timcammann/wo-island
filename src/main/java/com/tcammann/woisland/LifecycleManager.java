package com.tcammann.woisland;

import com.tcammann.woisland.feature.Listener;
import com.tcammann.woisland.feature.ranking.model.TimeframeOption;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class LifecycleManager<T extends Event> {
    final GatewayDiscordClient gatewayDiscordClient;
    final List<Listener<T>> listeners;
    final String reactionRankingCommandName;
    final String reactionRankingCommandDescription;
    final List<RegisteredCommand> registeredCommands = new ArrayList<>();
    final long applicationId;
    private final List<Long> reactionRankingServerIds;
    Logger LOG = LoggerFactory.getLogger(LifecycleManager.class);

    public LifecycleManager(
            @Value("#{${events.reaction.ranking.server-ids}}") List<Long> reactionRankingServerIds,
            GatewayDiscordClient gatewayDiscordClient,
            List<Listener<T>> listeners,
            @Value("${events.reaction.ranking.command-name}") String commandName,
            @Value("${events.reaction.ranking.command-description}") String commandDescription
    ) {
        this.reactionRankingServerIds = reactionRankingServerIds;
        this.gatewayDiscordClient = gatewayDiscordClient;
        this.listeners = listeners;
        this.reactionRankingCommandName = commandName;
        this.reactionRankingCommandDescription = commandDescription;
        this.applicationId = gatewayDiscordClient.getRestClient().getApplicationId().blockOptional().orElseThrow();
        LOG.info("Running {}.", this.getClass().getSimpleName());
    }

    @EventListener(ApplicationReadyEvent.class)
    public void register() {

        LOG.info("Subscribing event listeners.");
        Flux.fromIterable(listeners)
                .flatMap(listener -> gatewayDiscordClient.on(listener.getEventType())
                        .flatMap(event -> listener.execute(event)
                                .onErrorResume(listener::handleError))
                        .doOnSubscribe(
                                subscription -> LOG.info("{} subscribed to {}", listener.getClass().getSimpleName(), listener.getEventType().getSimpleName()))
                        .doOnComplete(() -> LOG.warn("{} terminated!", listener.getClass().getSimpleName())))
                .subscribe();

        LOG.info("Registering application commands.");
        var choices = Arrays.stream(TimeframeOption.values())
                .map(option -> ApplicationCommandOptionChoiceData.builder()
                        .name(option.displayName)
                        .value(option.name()).build())
                .toList();
        var commandRequest = ApplicationCommandRequest.builder()
                .name(reactionRankingCommandName)
                .description(reactionRankingCommandDescription)
                .addOption(ApplicationCommandOptionData.builder()
                        .name("timeframe")
                        .description("the current 'year', 'month' (default) or 'day'")
                        .choices(choices)
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(false)
                        .build())
                .build();

        Flux.fromIterable(reactionRankingServerIds)
                .flatMap(serverId -> gatewayDiscordClient.getRestClient().getApplicationService()
                        .createGuildApplicationCommand(applicationId, serverId, commandRequest)
                        .doOnSuccess(command -> {
                            registeredCommands.add(new RegisteredCommand(serverId, command.id().asLong()));
                            LOG.info("Application command '{}' registered at server '{}'", command.name(), serverId);
                        })
                        .doOnError(error -> LOG.error("Failed to register application command {} at server '{}'", commandRequest.name(), serverId, error)))
                .subscribe();
    }

    @PreDestroy
    public void unregister() {
        Flux.fromIterable(registeredCommands)
                .flatMap(c -> gatewayDiscordClient.getRestClient().getApplicationService()
                        .deleteGuildApplicationCommand(applicationId, c.serverId, c.commandId).thenReturn(c)
                        .doOnSuccess(command -> LOG.info("Application command '{}' deleted from server '{}'", reactionRankingCommandName, command.serverId)))
                .blockLast();
    }

    private record RegisteredCommand(long serverId, long commandId) {
    }
}
