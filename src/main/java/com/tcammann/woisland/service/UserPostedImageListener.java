package com.tcammann.woisland.service;

import com.tcammann.woisland.util.MessageUtils;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.discordjson.Id;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class UserPostedImageListener implements EventListener<MessageCreateEvent> {
    private final static Logger LOG = LoggerFactory.getLogger(UserPostedImageListener.class);
    public static final String CHARS_CONTAINED_IN_CONTENT_TYPE = "image";
    private final List<String> monitoredChannels;
    private final List<String> usernamesToReactTo;
    private final ReactionEmoji reactionEmoji;


    public UserPostedImageListener(
            @Value("${events.user-posted-image.emoji.id}") final String reactionEmojiId,
            @Value("${events.user-posted-image.emoji.name}") final String reactionEmojiName,
            @Value("#{${events.user-posted-image.user-names}}") final List<String> usernamesToReactTo,
            @Value("#{${events.user-posted-image.channel-ids}}") final List<String> monitoredChannels
    ) {
        this.usernamesToReactTo = usernamesToReactTo;
        this.monitoredChannels = monitoredChannels;
        this.reactionEmoji = ReactionEmoji.custom(Snowflake.of(Id.of(reactionEmojiId)), reactionEmojiName, false);
        LOG.info("Starting user posted image event listener for user names {}.", usernamesToReactTo);
    }

    @Override
    public Class<MessageCreateEvent> getEventType() {
        return MessageCreateEvent.class;
    }

    @Override
    public Mono<Void> execute(final MessageCreateEvent event) {
        return Mono.just(event.getMessage())
                .filter(message -> MessageUtils.isInChannel(message, monitoredChannels))
                .filter(this::isUserToReactTo)
                .filter(this::hasImageAttached)
                .flatMap(message -> message.addReaction(reactionEmoji))
                .then();
    }

    private boolean hasImageAttached(Message message) {
        return message.getAttachments().stream()
                .filter(attachment -> attachment.getContentType().isPresent())
                .anyMatch(attachment -> attachment.getContentType().get().contains(CHARS_CONTAINED_IN_CONTENT_TYPE));
    }

    private boolean isUserToReactTo(final Message message){
        if (message.getAuthor().isEmpty()){
            return false;
        }

        return usernamesToReactTo.stream()
                .anyMatch(username -> username.equals(message.getAuthor().get().getUsername()));
    }
}
