package com.tcammann.woisland.feature.ranking.model;

import discord4j.core.object.entity.Member;

public record RankedMember(Member member, Ranking ranking) {
}
