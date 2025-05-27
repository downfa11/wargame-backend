package com.ns.player.application.port.out;


import com.ns.player.adapter.out.persistence.Player;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FindPlayerPort {
    Mono<Player> findByMembershipId(String membershipId);
    Flux<Player> findAll();
}