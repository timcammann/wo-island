package com.tcammann.woisland.model;

public class Ranking {
    private int rank;
    private Long user;
    private Long count;

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
