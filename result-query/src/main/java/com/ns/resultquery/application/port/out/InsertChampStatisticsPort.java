package com.ns.resultquery.application.port.out;

import reactor.core.publisher.Mono;

public interface InsertChampStatisticsPort {
    Mono<Void> insertResultCountIncreaseEventByChampName(Long champIndex, String champName, Long resultCount, Long winCount, Long loseCount);

}
