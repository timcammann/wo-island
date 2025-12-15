package com.tcammann.woisland.service;

import com.tcammann.woisland.model.ReactionEventEntity;
import com.tcammann.woisland.repository.ReactionEventRepository;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ReactionEventListener implements EventListener<ReactionAddEvent> {

    private final ReactionEventRepository reactionEventRepository;
    private final List<String> trackedEmojis;

    public ReactionEventListener(
            ReactionEventRepository reactionEventRepository,
            @Value("#{${events.reaction.tracked-emojis}}") final List<String> trackedEmojis) {
        this.reactionEventRepository = reactionEventRepository;
        this.trackedEmojis = trackedEmojis;
    }

    @Override
    public Class<ReactionAddEvent> getEventType() {
        return ReactionAddEvent.class;
    }

    // TODO: Track ReactionRemoveEvents as well
    @Override
    public Mono<Void> execute(ReactionAddEvent event) {
        LOG.debug("Received reaction event with emoji {} by user {}.", event.getEmoji(), event.getMember().map(Object::toString).orElse("na"));
        return Mono.just(event)
                .filter(this::isTrackedReaction)
                .flatMap(this::toEntity)
                .flatMap(this::writeReactionEvent)
                .then();
    }

    private boolean isTrackedReaction(ReactionAddEvent event) {
        return event.getEmoji().asUnicodeEmoji().map(Object::toString).map(trackedEmojis::contains).orElse(false);
    }

    public Mono<ReactionEventEntity> toEntity(ReactionAddEvent event) throws NoSuchElementException {
        var serverId = event.getGuildId().map(Object::toString).orElseThrow();
        var reactionBy = event.getMember().map(Object::toString).orElse("na");
        var emoji = event.getEmoji().asUnicodeEmoji().map(Object::toString).orElseThrow();

        return event.getMessage()
                .flatMap(Message::getAuthorAsMember)
                .map(Object::toString)
                .map(reactionTo -> new ReactionEventEntity(serverId, reactionTo, reactionBy, emoji));
    }

    private Mono<ReactionEventEntity> writeReactionEvent(ReactionEventEntity reactionEventEntity) {
        return Mono.fromCallable(() -> reactionEventRepository.save(reactionEventEntity))
                .subscribeOn(Schedulers.boundedElastic());
    }

    // for debugging
    //    private Mono<ReactionEventEntity> readReactionEvent(Long id) {
    //        return Mono.fromCallable(() -> reactionEventRepository.findById(id))
    //                .subscribeOn(Schedulers.boundedElastic())
    //                .flatMap(optional -> optional.map(Mono::just)
    //                        .orElseGet(() -> Mono.error(new IllegalArgumentException("Failed to read reaction event (id: " + id + ")"))))
    //                .doOnNext(System.out::println);
    //    }
}
