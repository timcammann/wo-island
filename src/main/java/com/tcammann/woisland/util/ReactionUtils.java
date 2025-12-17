package com.tcammann.woisland.util;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.Member;

import java.util.List;
import java.util.Optional;

public class ReactionUtils {
    public static Optional<String> readEmoji(ReactionAddEvent event) {
        return event.getEmoji().asEmojiData().name().flatMap(ReactionUtils::asCodePoints);
    }

    public static Optional<Long> readMember(ReactionAddEvent event) {
        return event.getMember().map(Member::getId).map(Snowflake::asLong);
    }

    public static Optional<Long> readServer(ReactionAddEvent event) {
        return event.getGuildId().map(Snowflake::asLong);
    }

    private static Optional<String> asCodePoints(String string) {
        return string.codePoints()
                .mapToObj(codePoint -> String.format("%X", codePoint))
                .filter(codePoint -> !"FE0F".equalsIgnoreCase(codePoint)) // remove emoji-style codepoint
                .filter(codePoint -> !"FE0E".equalsIgnoreCase(codePoint)) // remove text-style codepoint
                .reduce("%s %s"::formatted)
                .map(codePoints -> "U+" + codePoints.trim());
    }

    public static List<String> emojiNamesAsCodePoints(List<String> customEmojiNames) {
        return customEmojiNames.stream().map(name -> ReactionUtils.asCodePoints(name).orElseThrow()).toList();
    }
}
