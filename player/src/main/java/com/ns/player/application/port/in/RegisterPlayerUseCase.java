package com.ns.player.application.port.in;

import com.ns.player.adapter.axon.query.QueryPlayer;
import reactor.core.publisher.Mono;

public interface RegisterPlayerUseCase {
    Mono<QueryPlayer> createPlayer(String membershipId, String nickname);
}
