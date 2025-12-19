package com.tcammann.woisland.service;

import com.tcammann.woisland.model.RankedMember;
import com.tcammann.woisland.model.Ranking;
import com.tcammann.woisland.model.Timeframe;
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

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Service
public class RankingCommandListener implements Listener<ChatInputInteractionEvent> {
    private final ReactionEventRepository reactionEventRepository;
    private final String commandName;
    private final String resultHeading;
    private final String resultLineTemplate;
    private final Integer pageSize;
    private final GatewayDiscordClient gatewayDiscordClient;

    public RankingCommandListener(
            ReactionEventRepository reactionEventRepository,
            @Value("${events.reaction.ranking.command-name}") String commandName,
            @Value("${events.reaction.ranking.result.page-size}") Integer pageSize,
            @Value("${events.reaction.ranking.result.heading}") String resultHeading,
            @Value("${events.reaction.ranking.result.line-template}") String resultLineTemplate,
            GatewayDiscordClient gatewayDiscordClient) {
        this.reactionEventRepository = reactionEventRepository;
        this.commandName = commandName;
        this.resultHeading = resultHeading;
        this.resultLineTemplate = resultLineTemplate;
        this.pageSize = pageSize;
        this.gatewayDiscordClient = gatewayDiscordClient;
        LOG.info("Starting {}.", this.getClass().getSimpleName());
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

        Date after;
        try {
            var timeframe = event.getOptionAsString("timeframe")
                    .map( option -> Timeframe.valueOf(option.trim().toUpperCase()))
                    .orElse(Timeframe.MONTH);
            after = calculateTimestampCutoff(timeframe);
        } catch (IllegalArgumentException e) {
            return event.reply().withContent("Timeframe not supported.");
        }

        var server = event.getInteraction().getGuildId().orElseThrow().asLong();
        return fetchRankings(server, after)
                .flatMap(rankings -> fetchMembers(rankings, server))
                .map(this::buildResponse)
                .flatMap(response -> event.reply().withContent(response));
    }

    private static Date calculateTimestampCutoff(Timeframe timeframe) {
        LocalDate now = LocalDate.now();
        LocalDate firstDayOfPeriod = switch (timeframe){
            case DAY -> now;
            case MONTH -> now.withDayOfMonth(1);
            case YEAR -> now.withDayOfYear(1);
        };
        return Date.from(firstDayOfPeriod.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
    }

    private Mono<List<Ranking>> fetchRankings(Long server, Date after) {
        return Mono.fromCallable(() -> {
                    var rankings = reactionEventRepository.findTopXByServer(server, after, Pageable.ofSize(pageSize)).getContent();
                    for (int i = 0; i < rankings.size(); i++) {
                        rankings.get(i).setRank(i + 1);
                    }
                    return rankings;
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
        var sb = new StringBuilder().append("%s\n".formatted(resultHeading));
        var lineTemplate = "%s\n".formatted(resultLineTemplate);
        for (var rankedMember : rankedMembers) {
            sb.append(lineTemplate.formatted(rankedMember.ranking().getRank(), rankedMember.member().getDisplayName(), rankedMember.ranking().getCount()));
        }
        return sb.toString();
    }
}


