package com.ns.result.application.port.out.cache;

import com.ns.result.adapter.out.persistence.elasticsearch.Result;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface PushRedisPort {
    Flux<Long> pushString(String key, List<String> values);
    Mono<Result> pushResult(String key, Result result);
}
