package com.ns.result.application.port.in;

import com.ns.common.GameFinishedEvent;
import com.ns.result.adapter.out.persistence.elasticsearch.Result;
import reactor.core.publisher.Mono;

public interface RegisterResultUseCase {
    Mono<Result> createResultTemp();
    Mono<Result> saveResult(GameFinishedEvent gameFinishedEvent);
}
