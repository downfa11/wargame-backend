package com.ns.result.application.port.out.search;

import com.ns.common.GameFinishedEvent;
import com.ns.result.adapter.out.persistence.elasticsearch.Result;
import reactor.core.publisher.Mono;

public interface RegisterResultPort {
    Mono<Result> saveResult(GameFinishedEvent gameFinishedEvent);
}
