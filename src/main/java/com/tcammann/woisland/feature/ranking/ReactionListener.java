package com.tcammann.woisland.feature.ranking;

import com.tcammann.woisland.feature.Listener;
import com.tcammann.woisland.feature.ranking.model.ReactionEventEntity;
import com.tcammann.woisland.feature.ranking.repository.ReactionEventRepository;
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
        LOG.info("Initializing {}.", this.getClass().getSimpleName());
    }

    @Override
    public Class<ReactionAddEvent> getEventType() {
        return ReactionAddEvent.class;
    }

    @Override
    public Mono<Void> execute(ReactionAddEvent event) {
        if (!isTrackedReaction(event)) {
            LOG.trace("Discarding reaction event. Untracked emoji. Emoji codepoint(s): {}", ReactionUtils.readEmojiAsCodePoints(event).orElse(null));
            return Mono.empty();
        }
        if (event.getMessageAuthorId().asLong() == ReactionUtils.readMemberId(event)) {
            LOG.trace("Discarding reaction event. Reaction to user's own message. Emoji codepoint(s): {}",
                    ReactionUtils.readEmojiAsCodePoints(event).orElse(null));
            return Mono.empty();
        }

        return writeReactionEvent(toEntity(event))
                .map(ReactionEventEntity::getId)
                .flatMap(this::fetchReactionEvent)
                .doOnNext(entity -> LOG.debug("Reaction event saved with emoji codepoint(s) {}", entity.getEmoji()))
                .then();
    }

    private boolean isTrackedReaction(ReactionAddEvent event) {
        return trackedEmojis.contains(ReactionUtils.readEmojiAsCodePoints(event).orElseThrow());
    }

    public ReactionEventEntity toEntity(ReactionAddEvent event) throws NoSuchElementException {
        var server = ReactionUtils.readServerId(event).orElseThrow();
        var message = event.getMessageId().asLong();
        var messageAuthor = event.getMessageAuthorId().asLong();
        var member = ReactionUtils.readMemberId(event);
        var emoji = ReactionUtils.readEmojiAsCodePoints(event).orElseThrow();

        return new ReactionEventEntity(server, message, messageAuthor, member, emoji);
    }

    private Mono<ReactionEventEntity> writeReactionEvent(ReactionEventEntity reactionEventEntity) {
        return Mono.fromCallable(() -> reactionEventRepository.save(reactionEventEntity))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<ReactionEventEntity> fetchReactionEvent(Long id) {
        return Mono.fromCallable(() -> reactionEventRepository.findById(id))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(optional -> optional.map(Mono::just)
                        .orElseGet(() -> Mono.error(new IllegalArgumentException("Failed to read reaction event (id: " + id + ")"))))
                .doOnNext(entity -> LOG.trace("Fetched reaction event entity: {}", entity));
    }
}
