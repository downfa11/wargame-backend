package com.ns.result.application.port.out.cache;

import com.ns.result.adapter.out.persistence.elasticsearch.Result;
import reactor.core.publisher.Flux;

public interface FindRedisPort {
    Flux<Result> findResultInRange(String key, int offset);
}
