package com.tcammann.woisland.service;

import com.tcammann.woisland.model.ReactionEvent;
import com.tcammann.woisland.model.ReactionEvent.ReactionType;
import com.tcammann.woisland.repository.ReactionEventRepository;
import com.tcammann.woisland.util.MessageUtils;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.reaction.ReactionEmoji;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class TestEventListener implements EventListener<MessageCreateEvent> {
    private final static Logger LOG = LoggerFactory.getLogger(TestEventListener.class);
    private final List<String> islandChannelIds;
    private final ReactionEventRepository reactionEventRepository;

    public TestEventListener(@Value("#{${events.test.channel-ids}}") final List<String> islandChannelIds, ReactionEventRepository reactionEventRepository) {
        this.islandChannelIds = islandChannelIds;
        this.reactionEventRepository = reactionEventRepository;
    }


    @Override
    public Class<MessageCreateEvent> getEventType() {
        return MessageCreateEvent.class;
    }

    @Override
    public Mono<Void> execute(final MessageCreateEvent event) {

        return Mono.just(event.getMessage())
                .filter(message -> MessageUtils.isInChannel(message, islandChannelIds))
                .flatMap(message -> message.addReaction(ReactionEmoji.codepoints("U+1f642")).thenReturn(message))
                .flatMap(this::writeReactionEvent)
                .flatMap(reactionEvent -> readReactionEvent(reactionEvent.getId()))
                .then();

    }

    private Mono<ReactionEvent> writeReactionEvent(Message message) {
        return Mono.fromCallable(() ->
                        reactionEventRepository.save(ReactionEvent.fromMessage(message)))
                .subscribeOn(Schedulers.boundedElastic());
    }
    private Mono<ReactionEvent> readReactionEvent(Long id) {
        return Mono.fromCallable(() -> reactionEventRepository.findById(id))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(optional -> optional.map(Mono::just)
                        .orElseGet(() -> Mono.error(new IllegalArgumentException("Failed to read reaction event (id: " + id + ")"))))
                .doOnNext(System.out::println);
    }
}

