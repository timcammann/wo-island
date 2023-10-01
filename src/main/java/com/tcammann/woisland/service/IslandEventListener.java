package com.tcammann.woisland.service;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Service
public class IslandEventListener implements EventListener<MessageCreateEvent> {
    private final static Logger LOG = LoggerFactory.getLogger(IslandEventListener.class);
    private final String islandEmojiCodepoints;
    private final List<String> islandChannelIds;
    private final List<String> islandKeywords;

    public IslandEventListener(
            @Value("${events.wo-island.emoji.codepoints}") final String islandEmojiCodepoints,
            @Value("#{${events.wo-island.channel-ids}}") final List<String> islandChannelIds,
            @Value("#{${events.wo-island.key-words}}") final List<String> islandKeywords) {
        this.islandEmojiCodepoints = islandEmojiCodepoints;
        this.islandChannelIds = islandChannelIds;
        this.islandKeywords = islandKeywords;
        LOG.info("Starting island event listener in channels {} and for keywords {}.", islandChannelIds, islandKeywords);
    }

    @Override
    public Class<MessageCreateEvent> getEventType() {
        return MessageCreateEvent.class;
    }

    @Override
    public Mono<Void> execute(final MessageCreateEvent event) {
        return Mono.just(event.getMessage())
                .filter(this::isIslandMessage)
                .flatMap(message -> message.addReaction(ReactionEmoji.codepoints(islandEmojiCodepoints)))
                .then();
    }

    private boolean isIslandMessage(final Message message){
        return isInIslandChannel(message) && containsIslandKeyword(message);
    }

    private boolean containsIslandKeyword(final Message message){
        return islandKeywords.stream().anyMatch(keyword -> message.getContent().contains(keyword));
    }

    private boolean isInIslandChannel(final Message message){
        MessageChannel messageChannel = message.getChannel().block();
        return Optional.ofNullable(messageChannel)
                .map(channel -> listContains(islandChannelIds, channel.getId().asString()))
                .orElse(false);
    }

    private boolean listContains(final List<String> argList, final String argString){
        return argList.stream().anyMatch(element -> element.equals(argString));
    }
}
