package com.tcammann.woisland.feature.reacting;

import com.tcammann.woisland.feature.Listener;
import com.tcammann.woisland.util.MessageUtils;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.emoji.Emoji;
import discord4j.core.object.entity.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class IslandListener implements Listener<MessageCreateEvent> {
    private final static Logger LOG = LoggerFactory.getLogger(IslandListener.class);
    private final String reactionEmojiCodePoints;
    private final List<String> islandChannelIds;
    private final List<String> islandKeywords;

    public IslandListener(
            @Value("${events.wo-island.emoji.codepoints}") final String reactionEmojiCodePoints,
            @Value("#{${events.wo-island.channel-ids}}") final List<String> islandChannelIds,
            @Value("#{${events.wo-island.key-words}}") final List<String> islandKeywords) {
        this.reactionEmojiCodePoints = reactionEmojiCodePoints;
        this.islandChannelIds = islandChannelIds;
        this.islandKeywords = islandKeywords;
        LOG.info("Initializing {} for channels {} and for keywords {}.", this.getClass().getSimpleName(), islandChannelIds, islandKeywords);
    }

    @Override
    public Class<MessageCreateEvent> getEventType() {
        return MessageCreateEvent.class;
    }

    @Override
    public Mono<Void> execute(final MessageCreateEvent event) {
        return Mono.just(event.getMessage())
                .filter(this::isIslandMessage)
                .flatMap(message -> message.addReaction(Emoji.codepoints(reactionEmojiCodePoints)))
                .then();
    }

    private boolean isIslandMessage(final Message message) {
        return MessageUtils.isInChannel(message, islandChannelIds) && containsIslandKeyword(message);
    }

    private boolean containsIslandKeyword(final Message message) {
        return islandKeywords.stream().anyMatch(keyword -> message.getContent().contains(keyword));
    }
}
