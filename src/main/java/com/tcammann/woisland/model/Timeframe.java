package com.tcammann.woisland.model;

public enum Timeframe {
    DAY("today"),
    MONTH("this month"),
    YEAR ("this year");

    public final String currently;

    Timeframe(String currently) {
        this.currently = currently;
    }
}
