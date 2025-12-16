package com.tcammann.woisland.model;

import discord4j.core.object.entity.Member;

public record RankedMember(Member member, Ranking ranking) {
}
