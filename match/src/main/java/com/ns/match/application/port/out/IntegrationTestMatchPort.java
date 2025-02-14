package com.ns.match.application.port.out;

import reactor.core.publisher.Mono;

public interface IntegrationTestMatchPort {
    Mono<Void> requestIntegrationTest(Long memberId, String nickName, Long elo);
    Mono<Long> getRequestCount();
}
