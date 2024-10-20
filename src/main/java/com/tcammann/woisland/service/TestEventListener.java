package com.tcammann.woisland.service;

import com.tcammann.woisland.util.MessageUtils;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.reaction.ReactionEmoji;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class TestEventListener implements EventListener<MessageCreateEvent> {
    private final List<String> islandChannelIds;

    public TestEventListener(@Value("#{${events.test.channel-ids}}") final List<String> islandChannelIds) {
        this.islandChannelIds = islandChannelIds;
    }


    @Override
    public Class<MessageCreateEvent> getEventType() {
        return MessageCreateEvent.class;
    }

    @Override
    public Mono<Void> execute(final MessageCreateEvent event) {

        return Mono.just(event.getMessage())
                .filter(message -> MessageUtils.isInChannel(message, islandChannelIds))
                .flatMap(message -> message.addReaction(ReactionEmoji.codepoints("U+1f642")))
                .then();
    }
}
