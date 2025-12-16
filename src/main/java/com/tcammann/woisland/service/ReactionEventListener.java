package com.tcammann.woisland.service;

import com.tcammann.woisland.model.ReactionEventEntity;
import com.tcammann.woisland.repository.ReactionEventRepository;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.reaction.ReactionEmoji;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.security.InvalidKeyException;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ReactionEventListener implements EventListener<ReactionAddEvent> {

    private final ReactionEventRepository reactionEventRepository;
    private final Integer trackedEmoji;

    public ReactionEventListener(
            ReactionEventRepository reactionEventRepository,
            @Value("#{${events.reaction.ranking.tracked-emoji}}") final Integer trackedEmoji) {
        this.reactionEventRepository = reactionEventRepository;
        this.trackedEmoji = trackedEmoji;
    }

    @Override
    public Class<ReactionAddEvent> getEventType() {
        return ReactionAddEvent.class;
    }

    // TODO: Track ReactionRemoveEvents as well, or otherwise don't allow duplicate reactions
    @Override
    public Mono<Void> execute(ReactionAddEvent event) {
        LOG.debug("Received reaction event with emoji {} by user {}.",
                event.getEmoji().asUnicodeEmoji().map(ReactionEmoji.Unicode::hashCode).orElseThrow(),
                event.getMember().map(Member::getId).map(Snowflake::asLong).orElseThrow());
        return Mono.just(event)
                .filter(this::isTrackedReaction)
                .flatMap(this::toEntity)
                .flatMap(this::writeReactionEvent)
                .then();
    }

    private boolean isTrackedReaction(ReactionAddEvent event) {
        return event.getEmoji().asUnicodeEmoji().map(ReactionEmoji.Unicode::hashCode).map(trackedEmoji::equals).orElse(false);
    }

    public Mono<ReactionEventEntity> toEntity(ReactionAddEvent event) throws NoSuchElementException {
        var serverId = event.getGuildId().map(Snowflake::asLong).orElseThrow();
        var reactionBy = event.getMember().map(Member::getId).map(Snowflake::asLong).orElseThrow();
        var emoji = event.getEmoji().asUnicodeEmoji().map(ReactionEmoji.Unicode::hashCode).orElseThrow();

        return event.getMessage()
                .map(message -> message.getAuthor().orElseThrow().getId().asLong())
                .map(reactionTo -> new ReactionEventEntity(serverId, reactionTo, reactionBy, emoji))
                .flatMap(this::writeReactionEvent)
                .map(ReactionEventEntity::getId)
                .flatMap(this::readReactionEvent);
    }

    private Mono<ReactionEventEntity> writeReactionEvent(ReactionEventEntity reactionEventEntity) {
        return Mono.fromCallable(() -> reactionEventRepository.save(reactionEventEntity))
                .subscribeOn(Schedulers.boundedElastic());
    }

        private Mono<ReactionEventEntity> readReactionEvent(Long id) {
            return Mono.fromCallable(() -> reactionEventRepository.findById(id))
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap(optional -> optional.map(Mono::just)
                            .orElseGet(() -> Mono.error(new IllegalArgumentException("Failed to read reaction event (id: " + id + ")"))))
                    .doOnNext(System.out::println);
        }
}
