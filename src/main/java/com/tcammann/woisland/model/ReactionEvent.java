package com.tcammann.woisland.model;

import discord4j.core.object.entity.Message;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.util.Date;

@Entity
public class ReactionEvent {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;
    private ReactionType reactionType;
    private Date date;
    private String userName;

    public ReactionEvent(ReactionType reactionType, String userName) {
        this.reactionType = reactionType;
        this.date = new Date();
        this.userName = userName;
    }

    public ReactionEvent() {

    }

    public Long getId() {
        return id;
    }

    public ReactionType getType() {
        return reactionType;
    }

    public void setType(ReactionType reactionType) {
        this.reactionType = reactionType;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public static ReactionEvent fromMessage(Message message) {
        return new ReactionEvent(ReactionType.OTHER, message.getUserData().username());
    }

    @Override
    public String toString() {
        return "ReactionEvent{" +
                "id=" + id +
                ", reactionType=" + reactionType +
                ", date=" + date +
                ", userName='" + userName + '\'' +
                '}';
    }

    public enum ReactionType {
        KEYWORD,
        MENTION,
        IMAGE,
        OTHER
    }
}
