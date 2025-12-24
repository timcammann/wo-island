package com.tcammann.woisland.model;

public enum TimeframeOption {
    TODAY("today","today"),
    THIS_MONTH("this-month", "this month"),
    THIS_YEAR ("this-year", "this year"),
    YESTERDAY("yesterday","yesterday"),
    LAST_MONTH("last-month", "last month"),
    LAST_YEAR ("last-year", "last year");

    public final String displayName;
    public final String text;

    TimeframeOption(String displayName, String text) {
        this.displayName = displayName;
        this.text = text;
    }

}
