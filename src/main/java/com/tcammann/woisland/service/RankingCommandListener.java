package com.tcammann.woisland.service;

import com.tcammann.woisland.model.RankedMember;
import com.tcammann.woisland.model.Ranking;
import com.tcammann.woisland.repository.ReactionEventRepository;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Service
public class RankingCommandListener implements Listener<ChatInputInteractionEvent> {

    private final ReactionEventRepository reactionEventRepository;
    private final String commandName;
    private final Integer pageSize;
    private final GatewayDiscordClient gatewayDiscordClient;

    public RankingCommandListener(
            ReactionEventRepository reactionEventRepository,
            @Value("${events.reaction.ranking.command-name}") String commandName,
            @Value("${events.reaction.ranking.page-size}") Integer pageSize,
            GatewayDiscordClient gatewayDiscordClient) {
        this.reactionEventRepository = reactionEventRepository;
        this.commandName = commandName;
        this.pageSize = pageSize;
        this.gatewayDiscordClient = gatewayDiscordClient;
    }

    @Override
    public Class<ChatInputInteractionEvent> getEventType() {
        return ChatInputInteractionEvent.class;
    }

    @Override
    public Mono<Void> execute(ChatInputInteractionEvent event) {
        LOG.debug("Received chat input interaction event {}.", event.getCommandName());
        if (!commandName.equals(event.getCommandName())) {
            return Mono.empty();
        }

        var server = event.getInteraction().getGuildId().orElseThrow().asLong();
        return fetchRankings(server)
                .flatMap(rankings -> fetchMembers(rankings, server))
                .map(this::buildResponse)
                .flatMap(response -> event.reply().withContent(response));
    }

    private Mono<List<Ranking>> fetchRankings(Long server) {
        return Mono.fromCallable(() -> {
                    var list = reactionEventRepository.findTopXByServer(server, Pageable.ofSize(pageSize)).getContent();
                    for (int i = 0; i < list.size(); i++) {
                        list.get(i).setRank(i + 1);
                    }
                    return list;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<List<RankedMember>> fetchMembers(List<Ranking> rankings, Long server) {
        return Flux.fromIterable(rankings)
                .flatMapSequential(ranking ->
                        gatewayDiscordClient.getMemberById(Snowflake.of(server), Snowflake.of(ranking.getUser()))
                                .map(member -> new RankedMember(member, ranking)))
                .collectList();
    }

    private String buildResponse(List<RankedMember> rankedMembers) {
        var sb = new StringBuilder().append("Most petted:\n"); // TODO: Make configurable
        var lineTemplate = "%s: %s with %s pets\n"; // TODO: Make configurable
        for (var rankedMember : rankedMembers) {
            sb.append(lineTemplate.formatted(rankedMember.ranking().getRank(), rankedMember.member().getDisplayName(), rankedMember.ranking().getCount()));
        }
        return sb.toString();
    }

}


