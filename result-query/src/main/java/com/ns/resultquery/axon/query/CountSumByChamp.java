package com.ns.resultquery.axon.query;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CountSumByChamp {
    private final String CountSumByChampId;
    private final String champName;

    private final Long champCount;
    private final Long winCount;
    private final Long loseCount;
    private final Long percent;
}
