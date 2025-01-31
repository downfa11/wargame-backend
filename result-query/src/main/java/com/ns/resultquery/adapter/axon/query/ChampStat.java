package com.ns.resultquery.adapter.axon.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ChampStat {
    private final Long champIndex;
    private final String champName;

    private final Long resultCount;
    private final Long winCount;
    private final Long loseCount;
    private final String percent;
}
