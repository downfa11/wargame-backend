package com.ns.player.application.port.out;

import com.ns.player.adapter.out.persistence.Player;
import reactor.core.publisher.Mono;

public interface RegisterPlayerPort {
    Mono<Player> registerPlayer(String membershipId, String aggregateIdentifier, String nickname);
}
