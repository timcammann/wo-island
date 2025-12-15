package com.tcammann.woisland.model;

import jakarta.persistence.*;

import java.util.Date;

@Entity
public class ReactionEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long server;
    private Long toUser;
    private Long byUser;
    private int emojiHash;
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    public ReactionEventEntity() {
    }

    public ReactionEventEntity(Long server, Long toUser, Long byUser, int emojiHash) {
        this.server = server;
        this.toUser = toUser;
        this.byUser = byUser;
        this.emojiHash = emojiHash;
        this.timestamp = new Date();
    }

    public Long getId() {
        return id;
    }

    public Long getServer() {
        return server;
    }

    public Long getToUser() {
        return toUser;
    }

    public Long getByUser() {
        return byUser;
    }

    public int getEmojiHash() {
        return emojiHash;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "ReactionEventEntity{" +
                "id=" + id +
                ", server=" + server +
                ", toUser=" + toUser +
                ", byUser=" + byUser +
                ", emojiHash=" + emojiHash +
                ", timestamp=" + timestamp +
                '}';
    }
}
