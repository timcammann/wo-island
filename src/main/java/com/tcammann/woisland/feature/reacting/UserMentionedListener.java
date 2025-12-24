package com.tcammann.woisland.feature.reacting;

import com.tcammann.woisland.feature.Listener;
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
public class UserMentionedListener implements Listener<MessageCreateEvent> {
    private final static Logger LOG = LoggerFactory.getLogger(UserMentionedListener.class);
    private final String reactionEmojiCodepoints;
    private final List<String> usernamesToReactTo;

    public UserMentionedListener(
            @Value("${events.user-mentioned.emoji.codepoints}") final String reactionEmojiCodepoints,
            @Value("#{${events.user-mentioned.user-names}}") final List<String> usernamesToReactTo) {
        this.reactionEmojiCodepoints = reactionEmojiCodepoints;
        this.usernamesToReactTo = usernamesToReactTo;
        LOG.info("Initializing {} for user names {}.", this.getClass().getSimpleName(), usernamesToReactTo);
    }

    @Override
    public Class<MessageCreateEvent> getEventType() {
        return MessageCreateEvent.class;
    }

    @Override
    public Mono<Void> execute(final MessageCreateEvent event) {
        return Mono.just(event.getMessage())
                .filter(this::userMentioned)
                .flatMap(message -> message.addReaction(Emoji.codepoints(reactionEmojiCodepoints)))
                .then();
    }

    private boolean userMentioned(final Message message) {
        return usernamesToReactTo.stream()
                .anyMatch(username -> message.getUserMentions().stream()
                        .anyMatch(user -> user.getUsername().equals(username)));
    }
}
