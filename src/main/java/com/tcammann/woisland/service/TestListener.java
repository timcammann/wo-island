package com.tcammann.woisland.service;

import com.tcammann.woisland.repository.ReactionEventRepository;
import com.tcammann.woisland.util.MessageUtils;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.reaction.ReactionEmoji;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class TestListener implements Listener<MessageCreateEvent> {
    private final static Logger LOG = LoggerFactory.getLogger(TestListener.class);
    private final List<String> islandChannelIds;
    private final ReactionEventRepository reactionEventRepository;

    public TestListener(@Value("#{${events.test.channel-ids}}") final List<String> islandChannelIds, ReactionEventRepository reactionEventRepository) {
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
                .then();

    }
}

