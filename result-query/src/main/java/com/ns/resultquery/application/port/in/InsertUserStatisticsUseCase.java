package com.ns.resultquery.application.port.in;

import com.ns.resultquery.domain.dto.MembershipResultEventDto;
import reactor.core.publisher.Mono;

public interface InsertUserStatisticsUseCase {
    Mono<Void> insertResultCountIncreaseEventByUserName(MembershipResultEventDto eventDto);
}
