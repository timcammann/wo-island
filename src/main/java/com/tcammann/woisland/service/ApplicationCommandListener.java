package com.tcammann.woisland.service;

import com.tcammann.woisland.repository.ReactionEventRepository;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ApplicationCommandListener implements EventListener<ChatInputInteractionEvent> {

    private final ReactionEventRepository reactionEventRepository;

    public ApplicationCommandListener(ReactionEventRepository reactionEventRepository) {
        this.reactionEventRepository = reactionEventRepository;
    }

    @Override
    public Class<ChatInputInteractionEvent> getEventType() {
        return ChatInputInteractionEvent.class;
    }


    @Override
    public Mono<Void> execute(ChatInputInteractionEvent event) {
        LOG.debug("Received chat input interaction event.");

        // TODO: Dispatch based on command, make reactive
        var result = reactionEventRepository.findTopXByServer(event.getInteraction().getGuildId().map(Snowflake::asLong).orElseThrow(), Pageable.ofSize(3));

        var lineTemplate = "Rank: %s - %s with %s pets\n";
        var sb = new StringBuilder().append("Most petted:\n");
        int rank = 1;
        for (var record : result.getContent()){
            sb.append(lineTemplate.formatted(rank, record.getToUser(), record.getCount()));
            rank++;
        }
        return event.reply().withContent(sb.toString());
    }
}
