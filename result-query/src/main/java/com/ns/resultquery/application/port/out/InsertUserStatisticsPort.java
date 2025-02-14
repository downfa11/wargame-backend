package com.ns.resultquery.application.port.out;

import com.ns.resultquery.domain.dto.InsertResultCountDto;
import reactor.core.publisher.Mono;

public interface InsertUserStatisticsPort {

    Mono<Void> insertResultCountIncreaseEventByUserName(Long membershipId, String username, InsertResultCountDto insertResultCountDto);
}
