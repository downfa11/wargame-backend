package com.ns.result.application.port.out.search;

import reactor.core.publisher.Mono;

public interface DeleteResultPort {
    Mono<Boolean> deleteResult(String spaceId);
}
