package com.tcammann.woisland.service;

import com.tcammann.woisland.model.ReactionEventEntity;
import com.tcammann.woisland.repository.ReactionEventRepository;
import com.tcammann.woisland.util.ReactionUtils;
import discord4j.core.event.domain.message.ReactionAddEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

@Service
public class ReactionListener implements Listener<ReactionAddEvent> {

    private final ReactionEventRepository reactionEventRepository;
    private final List<String> trackedEmojis;

    public ReactionListener(
            ReactionEventRepository reactionEventRepository,
            @Value("#{${events.reaction.ranking.tracked-emojis.custom.names}}") final List<String> customEmojiNames,
            @Value("#{${events.reaction.ranking.tracked-emojis.utf8.code-points}}") final List<String> utfEmojiCodePoints) {
        this.reactionEventRepository = reactionEventRepository;
        var customEmojiCodePoints = ReactionUtils.emojiNamesAsCodePoints(customEmojiNames);
        this.trackedEmojis = Stream.concat(customEmojiCodePoints.stream(), utfEmojiCodePoints.stream()).toList();
    }

    @Override
    public Class<ReactionAddEvent> getEventType() {
        return ReactionAddEvent.class;
    }

    // TODO: Track ReactionRemoveEvents as well, or otherwise don't allow duplicate reactions
    @Override
    public Mono<Void> execute(ReactionAddEvent event) {
        if (!isTrackedReaction(event)) {
            LOG.debug("Discarding reaction event with emoji codepoint(s) {}", ReactionUtils.readEmoji(event).orElse(null));
            return Mono.empty();
        }

        return Mono.just(event)
                .flatMap(this::toEntity)
                .flatMap(this::writeReactionEvent)
                .map(ReactionEventEntity::getId)
                .flatMap(this::readReactionEvent)
                .doOnNext(entity -> LOG.debug("Reaction event saved with emoji codepoint(s) {}", entity.getEmoji()))
                .then();
    }

    private boolean isTrackedReaction(ReactionAddEvent event) {
        return trackedEmojis.contains(ReactionUtils.readEmoji(event).orElseThrow());
    }

    public Mono<ReactionEventEntity> toEntity(ReactionAddEvent event) throws NoSuchElementException {
        var serverId = ReactionUtils.readServer(event).orElseThrow();
        var reactionBy = ReactionUtils.readMember(event).orElseThrow();
        var emoji = ReactionUtils.readEmoji(event).orElseThrow();

        return event.getMessage()
                .map(message -> message.getAuthor().orElseThrow().getId().asLong())
                .map(reactionTo -> new ReactionEventEntity(serverId, reactionTo, reactionBy, emoji));
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
