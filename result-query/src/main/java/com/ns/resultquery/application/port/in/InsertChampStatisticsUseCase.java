package com.ns.resultquery.application.port.in;

import com.ns.resultquery.domain.dto.ResultEventDto;
import reactor.core.publisher.Mono;

public interface InsertChampStatisticsUseCase {
    Mono<Void> insertResultCountIncreaseEventByChampName(ResultEventDto eventDto);
}
