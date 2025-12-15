package com.tcammann.woisland.model;

import jakarta.persistence.*;

@Entity
public class ReactionRanking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long toUser;
    private Long count;

    public ReactionRanking() {
    }

    public ReactionRanking(Long toUser, Long count) {
        this.toUser = toUser;
        this.count = count;
    }

    public Long getToUser() {
        return toUser;
    }

    public Long getCount() {
        return count;
    }

    @Override
    public String toString() {
        return "Ranking{" +
                ", toUser=" + toUser +
                ", count=" + count +
                '}';
    }
}
