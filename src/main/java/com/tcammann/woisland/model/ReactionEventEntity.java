package com.tcammann.woisland.model;

import jakarta.persistence.*;

import java.util.Date;

@Entity
public class ReactionEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long server;
    private Long messageAuthor; // the user that posted the message
    private Long member; // the user that reacted to the message
    private String emoji; // all emojis are stored as code points
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    public ReactionEventEntity() {
    }

    public ReactionEventEntity(Long server, Long messageAuthor, Long member, String emoji) {
        this.server = server;
        this.messageAuthor = messageAuthor;
        this.member = member;
        this.emoji = emoji;
        this.timestamp = new Date();
    }

    public Long getId() {
        return id;
    }

    public Long getServer() {
        return server;
    }

    public Long getMessageAuthor() {
        return messageAuthor;
    }

    public Long getMember() {
        return member;
    }

    public String getEmoji() {
        return emoji;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "ReactionEventEntity{" +
                "id=" + id +
                ", server=" + server +
                ", toUser=" + messageAuthor +
                ", byUser=" + member +
                ", emojiCodePoints=" + emoji +
                ", timestamp=" + timestamp +
                '}';
    }
}
