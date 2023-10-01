package com.tcammann.woisland.service;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

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
                .filter(this::isInTestChannel)
                .flatMap(message -> message.addReaction(ReactionEmoji.codepoints("U+1f642")))
                .then();
    }

    private boolean isInTestChannel(final Message message){
        MessageChannel messageChannel = message.getChannel().block();
        return Optional.ofNullable(messageChannel)
                .map(channel -> listContains(islandChannelIds, channel.getId().asString()))
                .orElse(false);
    }

    private boolean listContains(final List<String> argList, final String argString){
        return argList.stream().anyMatch(element -> element.equals(argString));
    }
}
