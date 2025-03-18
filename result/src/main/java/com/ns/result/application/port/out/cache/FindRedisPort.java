package com.ns.result.application.port.out.cache;

import com.ns.result.adapter.out.persistence.elasticsearch.Result;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;

public interface FindRedisPort {
    Flux<String> findString(String key);
    Flux<Result> findResultInRange(String key, int offset);
}
