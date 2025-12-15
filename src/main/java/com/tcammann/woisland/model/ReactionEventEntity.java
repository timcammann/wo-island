package com.tcammann.woisland.model;

import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.Message;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.util.Date;
import java.util.NoSuchElementException;

@Entity
public class ReactionEventEntity {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;
    private String serverId;
    private String to;
    private String by;
    private String emoji;
    private Date date;

    public ReactionEventEntity(String serverId, String to, String by, String emoji) {
        this.serverId = serverId;
        this.to = to;
        this.by = by;
        this.emoji = emoji;
        this.date = new Date();
    }

    public ReactionEventEntity() {

    }

    public Long getId() {
        return id;
    }

    public String getServerId() {
        return serverId;
    }

    public String getTo() {
        return to;
    }

    public String getBy() {
        return by;
    }

    public Date getDate() {
        return date;
    }

    public String getEmoji() {
        return emoji;
    }
}
