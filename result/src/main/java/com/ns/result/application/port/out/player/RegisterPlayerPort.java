package com.ns.result.application.port.out.player;

import com.ns.result.adapter.out.persistence.psql.Player;
import reactor.core.publisher.Mono;

public interface RegisterPlayerPort {
    Mono<Player> registerPlayer(String membershipId, String aggregateIdentifier);
}
