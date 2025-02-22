package com.ns.resultquery.adapter.axon.query;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CountSumByMembership {
    private final String username;

    private final Long entireCount;
    private final Long winCount;
    private final Long loseCount;

    private final List<ChampStat> champStatList;
}
