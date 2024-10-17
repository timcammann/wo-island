package com.tcammann.woisland.service;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.reaction.ReactionEmoji;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class UserMentionedListener implements EventListener<MessageCreateEvent> {
    private final static Logger LOG = LoggerFactory.getLogger(UserMentionedListener.class);
    private final String userMentionedEmojiCodepoints;
    private final List<String> userNamesToReactTo;

    public UserMentionedListener(
            @Value("${events.user-mentioned.emoji.codepoints}") final String userMentionedEmojiCodepoints,
            @Value("#{${events.user-mentioned.user-names}}") final List<String> userNamesToReactTo) {
        this.userMentionedEmojiCodepoints = userMentionedEmojiCodepoints;
        this.userNamesToReactTo = userNamesToReactTo;
        LOG.info("Starting user mention event listener for user names {}.", userNamesToReactTo);
    }

    @Override
    public Class<MessageCreateEvent> getEventType() {
        return MessageCreateEvent.class;
    }

    @Override
    public Mono<Void> execute(final MessageCreateEvent event) {
        return Mono.just(event.getMessage())
                .filter(this::userMentioned)
                .flatMap(message -> message.addReaction(ReactionEmoji.codepoints(userMentionedEmojiCodepoints)))
                .then();
    }

    private boolean userMentioned(final Message message){
        return userNamesToReactTo.stream()
                .anyMatch(keyword -> message.getUserMentions().stream()
                        .anyMatch(user -> user.getUsername().equals(keyword)));
    }
}
