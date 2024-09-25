package com.ns.resultquery.axon.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ChampStat {
    private final long champIndex;
    private final long champName;

    private final long winCount;
    private final long loseCount;
    private final long percent;
}
