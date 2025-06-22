package com.ns.player.adapter.out.persistence;

import com.ns.common.anotation.PersistanceAdapter;
import com.ns.player.application.port.out.FindPlayerPort;
import com.ns.player.application.port.out.RegisterPlayerPort;
import com.ns.player.application.port.out.UpdatePlayerPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@PersistanceAdapter
@RequiredArgsConstructor
public class PlayerPersistenceAdapter implements RegisterPlayerPort, UpdatePlayerPort, FindPlayerPort {
    private final PlayerR2dbcRepository playerRepository;

    @Override
    public Mono<Player> registerPlayer(String membershipId, String aggregateIdentifier,String nickname) {
        Player player = Player.builder()
                .membershipId(membershipId)
                .aggregateIdentifier(aggregateIdentifier)
                .nickname(nickname)
                .elo(2000L)
                .code("").build();

        return playerRepository.save(player);
    }

    @Override
    public Mono<Player> updatePlayerElo(String membershipId, Long newElo) {
        return playerRepository.findByMembershipId(membershipId)
                .flatMap(u -> {
                    u.setElo(newElo);
                    u.setCode(""); // if game is ended, Player's code blank.
                    u.setLastGameTime(LocalDateTime.now());

                    if (u.getTier() != Tier.RANKER) {
                        u.setTier(Tier.fromElo(newElo));
                    }

                    return playerRepository.save(u);
                });
    }

    @Override
    public Mono<Player> updateRankerTier(String membershipId, Tier tier) {
        return playerRepository.findByMembershipId(membershipId)
                .flatMap(u -> {
                    u.setTier(tier);
                    return playerRepository.save(u);
                });
    }

    @Override
    public Mono<Player> updatePlayerCode(String membershipId, String newCode) {
        return playerRepository.findByMembershipId(membershipId)
                .flatMap(u -> {
                    log.info("membership:"+membershipId+"'s new code is "+ newCode + "    prev:"+u.getCode());
                    u.setCode(newCode);
                    return playerRepository.save(u);
                });
    }

    @Override
    public Mono<Player> findByMembershipId(String membershipId) { return playerRepository.findByMembershipId(membershipId); }

    @Override
    public Flux<Player> findTopRankedPlayers(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return playerRepository.findTopPlayersByElo(pageable);
    }

    @Override
    public Flux<Player> findAll() { return playerRepository.findAll(); }


}
