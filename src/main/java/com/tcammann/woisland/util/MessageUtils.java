package com.tcammann.woisland.util;

import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;

import java.util.List;
import java.util.Optional;

public class MessageUtils {

    public static boolean isInChannel(final Message message, List<String> channelIds){
        MessageChannel messageChannel = message.getChannel().block();
        return Optional.ofNullable(messageChannel)
                .map(channel -> listContains(channelIds, channel.getId().asString()))
                .orElse(false);
    }

    private static boolean listContains(final List<String> argList, final String argString){
        return argList.stream().anyMatch(element -> element.equals(argString));
    }
}
