package com.ns.resultquery.axon.query;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CountSumByMembership {
    private final String membershipId;
    private final String name;

    private final long entireCount;
    private final long winCount;
    private final long loseCount;
    private final long percent;

    private final List<ChampStat> champStatList;
}
