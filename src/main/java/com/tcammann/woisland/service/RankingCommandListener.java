package com.tcammann.woisland.service;

import com.tcammann.woisland.model.RankedMember;
import com.tcammann.woisland.model.Ranking;
import com.tcammann.woisland.model.TimeframeOption;
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
import java.util.concurrent.TimeUnit;

@Service
public class RankingCommandListener implements Listener<ChatInputInteractionEvent> {
    private final ReactionEventRepository reactionEventRepository;
    private final String commandName;
    private final String headingTemplate;
    private final String resultLineTemplate;
    private final Integer pageSize;
    private final GatewayDiscordClient gatewayDiscordClient;

    public RankingCommandListener(
            ReactionEventRepository reactionEventRepository,
            @Value("${events.reaction.ranking.command-name}") String commandName,
            @Value("${events.reaction.ranking.result.page-size}") Integer pageSize,
            @Value("${events.reaction.ranking.result.heading}") String headingTemplate,
            @Value("${events.reaction.ranking.result.line-template}") String resultLineTemplate,
            GatewayDiscordClient gatewayDiscordClient) {
        this.reactionEventRepository = reactionEventRepository;
        this.commandName = commandName;
        this.headingTemplate = headingTemplate;
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

        TimeframeOption timeframe;
        Date after;
        try {
            timeframe = event.getOptionAsString("timeframe")
                    .map( option -> TimeframeOption.valueOf(option.trim().toUpperCase()))
                    .orElse(TimeframeOption.THIS_MONTH);
            after = calculateTimestampCutoff(timeframe);
        } catch (IllegalArgumentException e) {
            return event.reply().withContent("Timeframe not supported.");
        }

        var server = event.getInteraction().getGuildId().orElseThrow().asLong();
        return fetchRankings(server, after)
                .flatMap(rankings -> fetchMembers(rankings, server))
                .map(rankings -> buildReply(rankings, timeframe))
                .flatMap(response -> event.reply().withContent(response));
    }

    private static Date calculateTimestampCutoff(TimeframeOption timeframe) {
        LocalDate now = LocalDate.now();
        LocalDate firstDayOfPeriod = switch (timeframe){
            case TODAY -> now;
            case THIS_MONTH -> now.withDayOfMonth(1);
            case THIS_YEAR -> now.withDayOfYear(1);
        };
        return Date.from(firstDayOfPeriod.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
    }

    private Mono<List<Ranking>> fetchRankings(Long server, Date after) {
        var before = System.nanoTime();
        return Mono.fromCallable(() -> {
                    var rankings = reactionEventRepository.findTopXByServer(server, after, Pageable.ofSize(pageSize)).getContent();
                    for (int i = 0; i < rankings.size(); i++) {
                        rankings.get(i).setRank(i + 1);
                    }
                    return rankings;
                })
                .doOnSuccess(t -> LOG.trace("Fetched rankings from database in {}ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - before)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<List<RankedMember>> fetchMembers(List<Ranking> rankings, Long server) {
        return Flux.fromIterable(rankings)
                .flatMapSequential(ranking ->
                        gatewayDiscordClient.getMemberById(Snowflake.of(server), Snowflake.of(ranking.getUser()))
                                .map(member -> new RankedMember(member, ranking)))
                .collectList();
    }

    private String buildReply(List<RankedMember> rankedMembers, TimeframeOption timeframe) {
        var heading = (headingTemplate + "\n").formatted(timeframe.text);
        var lineTemplate = resultLineTemplate + "\n";

        var sb = new StringBuilder().append(heading);
        if (rankedMembers.isEmpty()){
            sb.append("nobody :'(\n");
        } else {
            for (var rankedMember : rankedMembers) {
                sb.append(lineTemplate.formatted(rankedMember.ranking().getRank(), rankedMember.member().getDisplayName(), rankedMember.ranking().getCount()));
            }
        }
        return sb.toString();
    }
}


