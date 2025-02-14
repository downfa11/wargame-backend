package com.ns.match.application.port.in;

import reactor.core.publisher.Mono;

public interface IntegrationTestMatchUseCase {
    Mono<Long> requestIntegrationTest(int threads, int requests);
}
