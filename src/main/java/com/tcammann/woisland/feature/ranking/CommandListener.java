package com.tcammann.woisland.feature.ranking;

import com.tcammann.woisland.feature.Listener;
import com.tcammann.woisland.feature.ranking.model.RankedMember;
import com.tcammann.woisland.feature.ranking.model.Ranking;
import com.tcammann.woisland.feature.ranking.model.TimeframeOption;
import com.tcammann.woisland.feature.ranking.repository.ReactionEventRepository;
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
public class CommandListener implements Listener<ChatInputInteractionEvent> {
    private final ReactionEventRepository reactionEventRepository;
    private final String commandName;
    private final String headingTemplate;
    private final String resultLineTemplate;
    private final Integer pageSize;
    private final GatewayDiscordClient gatewayDiscordClient;

    public CommandListener(
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
        LOG.info("Initializing {}.", this.getClass().getSimpleName());
    }

    private static StartAndEndDate calculateStartAndEndDate(TimeframeOption timeframe, LocalDate now) {
        final var aMonthAgo = now.minusMonths(1);
        final var aYearAgo = now.minusYears(1);

        var theFirstDay = switch (timeframe) {
            case TODAY -> now;
            case THIS_MONTH -> now.withDayOfMonth(1);
            case THIS_YEAR -> now.withDayOfYear(1);
            case YESTERDAY -> now.minusDays(1);
            case LAST_MONTH -> aMonthAgo.withDayOfMonth(1);
            case LAST_YEAR -> aYearAgo.withDayOfYear(1);
        };
        var startOfFirstDay = Date.from(theFirstDay.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());

        var theDayAfter = switch (timeframe) {
            case TODAY -> now.plusDays(1);
            case THIS_MONTH -> now.plusMonths(1).withDayOfMonth(1);
            case THIS_YEAR -> now.plusYears(1).withDayOfYear(1);
            case YESTERDAY -> now;
            case LAST_MONTH -> now.withDayOfMonth(1);
            case LAST_YEAR -> now.withDayOfYear(1);
        };
        var endOfLastDay = Date.from(theDayAfter.atStartOfDay().minusNanos(1).atZone(ZoneId.systemDefault()).toInstant());

        return new StartAndEndDate(startOfFirstDay, endOfLastDay);
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
        StartAndEndDate startAndEndDate;
        try {
            timeframe = event.getOptionAsString("timeframe")
                    .map(option -> TimeframeOption.valueOf(option.trim().toUpperCase()))
                    .orElse(TimeframeOption.THIS_MONTH);
            startAndEndDate = calculateStartAndEndDate(timeframe, LocalDate.now());
        } catch (IllegalArgumentException e) {
            return event.reply().withContent("Timeframe not supported.");
        }

        var server = event.getInteraction().getGuildId().orElseThrow().asLong();
        return fetchRankings(server, startAndEndDate)
                .flatMap(rankings -> fetchMembers(rankings, server))
                .map(rankings -> buildReply(rankings, timeframe))
                .flatMap(response -> event.reply().withContent(response));
    }

    private Mono<List<Ranking>> fetchRankings(Long server, StartAndEndDate startAndEndDate) {
        var before = System.nanoTime();
        return Mono.fromCallable(() -> {
                    var rankings = reactionEventRepository.findTopXByServer(server, startAndEndDate.start, startAndEndDate.end, Pageable.ofSize(pageSize)).getContent();
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
        if (rankedMembers.isEmpty()) {
            sb.append("nobody :'(\n");
        } else {
            for (var rankedMember : rankedMembers) {
                sb.append(lineTemplate.formatted(rankedMember.ranking().getRank(), rankedMember.member().getDisplayName(), rankedMember.ranking().getCount()));
            }
        }
        return sb.toString();
    }

    private record StartAndEndDate(Date start, Date end) {
    }
}


