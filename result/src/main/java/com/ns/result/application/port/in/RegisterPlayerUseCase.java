package com.ns.result.application.port.in;

import com.ns.result.adapter.axon.query.QueryPlayer;
import reactor.core.publisher.Mono;

public interface RegisterPlayerUseCase {
    Mono<QueryPlayer> createPlayer(String membershipId);
}
