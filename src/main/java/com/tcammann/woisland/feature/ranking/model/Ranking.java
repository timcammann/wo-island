package com.tcammann.woisland.feature.ranking.model;

public class Ranking {
    private final Long user;
    private final Long count;
    private int rank;

    public Ranking(Long user, Long count) {
        this.user = user;
        this.count = count;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public Long getUser() {
        return user;
    }

    public Long getCount() {
        return count;
    }

    @Override
    public String toString() {
        return "ReactionCount{" +
               "rank=" + rank +
               ", user=" + user +
               ", count=" + count +
               '}';
    }
}
