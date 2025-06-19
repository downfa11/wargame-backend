package com.ns.player.application.service;

import com.ns.common.CreatePlayerCommand;
import com.ns.common.anotation.UseCase;
import com.ns.player.adapter.axon.command.UpdateEloCommand;
import com.ns.player.adapter.axon.query.QueryPlayer;
import com.ns.player.adapter.out.persistence.Player;
import com.ns.player.adapter.out.persistence.Tier;
import com.ns.player.application.port.in.*;
import com.ns.player.application.port.out.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Slf4j
@UseCase
@RequiredArgsConstructor
public class PlayerService implements RegisterPlayerUseCase, UpdatePlayerUseCase, FindPlayerUseCase {
    private final RegisterPlayerPort registerPlayerPort;
    private final UpdatePlayerPort updatePlayerPort;
    private final FindPlayerPort findPlayerPort;

    private final SendCommandPort sendCommandPort;
    private final SendQueryPort sendQueryPort;

    @Override
    public Mono<QueryPlayer> createPlayer(String membershipId, String nickname) {
        CreatePlayerCommand playerCommand = new CreatePlayerCommand(membershipId, nickname);

        return sendCommandPort.sendCreatePlayer(playerCommand)
                .doOnError(throwable -> log.error("createMemberByEvent error: ", throwable))
                .then(Mono.defer(() -> queryToPlayerByMembershipId(membershipId)));
    }

    @Override
    public Mono<QueryPlayer> updateEloByEvent(String membershipId, Long balancedElo) {
        return findPlayerPort.findByMembershipId(membershipId)
                .flatMap(player -> sendCommandPort.sendUpdatePlayerElo(
                                new UpdateEloCommand(player.getAggregateIdentifier(), membershipId, balancedElo))
                        .then(updatePlayerPort.updatePlayerElo(membershipId, balancedElo))
                        .then(queryToPlayerByMembershipId(membershipId))
                )
                .switchIfEmpty(Mono.fromRunnable(() -> log.error("Not Found player {}", membershipId)))
                .doOnError(throwable -> log.error("PlayerService updateEloByEvent error", throwable));
    }


    @Override
    public Mono<Player> updateElo(String membershipId, Long newElo) {
        return updatePlayerPort.updatePlayerElo(membershipId, newElo);
    }

    @Override
    public Flux<Player> findAll() {
        return findPlayerPort.findAll();
    }

    @Override
    public Mono<QueryPlayer> queryToPlayerByMembershipId(String membershipId) {
        return sendQueryPort.sendPlayerQuery(membershipId);
    }

    @Override
    public Mono<PlayerInfo> queryToPlayerByNickname(String nickname) {
        return sendQueryPort.sendPlayerInfo(nickname);
    }

    @Override
    public Flux<RankPlayer> findTopRankedPlayers(int limit) {
        return findPlayerPort.findTopRankedPlayers(limit)
                .index() // (index, player)
                .map(tuple -> {
                    long index = tuple.getT1();
                    Player player = tuple.getT2();
                    return RankPlayer.builder()
                            .rank(index + 1)
                            .nickname(player.getNickname())
                            .tier(player.getTier().getName())
                            .elo(player.getElo())
                            .build();
                });
    }

}