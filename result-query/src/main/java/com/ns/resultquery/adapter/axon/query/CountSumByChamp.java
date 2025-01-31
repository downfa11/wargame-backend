package com.ns.resultquery.adapter.axon.query;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CountSumByChamp {
    private final String champName;
    private final Long champCount;
    private final Long winCount;
    private final Long loseCount;
}
