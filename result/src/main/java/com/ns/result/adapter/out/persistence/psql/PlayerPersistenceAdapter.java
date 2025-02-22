package com.ns.result.adapter.out.persistence.psql;

import com.ns.common.anotation.PersistanceAdapter;
import com.ns.result.application.port.out.player.FindPlayerPort;
import com.ns.result.application.port.out.player.RegisterPlayerPort;
import com.ns.result.application.port.out.player.UpdatePlayerPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@PersistanceAdapter
@RequiredArgsConstructor
public class PlayerPersistenceAdapter implements RegisterPlayerPort, UpdatePlayerPort, FindPlayerPort {
    private final PlayerR2dbcRepository playerRepository;

    @Override
    public Mono<Player> registerPlayer(String membershipId, String aggregateIdentifier) {
        Player player = Player.builder()
                .membershipId(membershipId)
                .aggregateIdentifier(aggregateIdentifier)
                .elo(2000L)
                .code("").build();

        return playerRepository.save(player);
    }

    @Override
    public Mono<Player> updatePlayer(String membershipId, Long increase) {
        return playerRepository.findByMembershipId(membershipId)
                .flatMap(u -> {
                    Long newElo = u.getElo() + increase;
                    u.setElo(newElo);
                    return playerRepository.save(u);
                });
    }

    @Override
    public Mono<Player> findByMembershipId(String membershipId) { return playerRepository.findByMembershipId(membershipId); }
    @Override
    public Flux<Player> findAll() { return playerRepository.findAll(); }


}
