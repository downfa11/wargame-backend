package com.ns.result.application.port.out.cache;

import com.ns.result.adapter.out.persistence.elasticsearch.Result;
import reactor.core.publisher.Mono;

public interface PushRedisPort {
    Mono<Result> pushResult(String key, Result result);
}
