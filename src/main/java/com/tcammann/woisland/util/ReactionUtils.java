package com.tcammann.woisland.util;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.discordjson.Id;

import java.util.Optional;

public class ReactionUtils {
    public static Optional<Long> readEmoji(ReactionAddEvent event) {
        return event.getEmoji().asEmojiData().id().map(Id::asLong)
                .or(() -> event.getEmoji().asUnicodeEmoji().map(ReactionEmoji.Unicode::hashCode).map(Integer::longValue));
    }

    public static Optional<Long> readMember(ReactionAddEvent event) {
        return event.getMember().map(Member::getId).map(Snowflake::asLong);
    }

    public static Optional<Long> readServer(ReactionAddEvent event) {
        return event.getGuildId().map(Snowflake::asLong);
    }
}
