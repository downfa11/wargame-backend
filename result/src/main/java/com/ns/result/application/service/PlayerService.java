package com.ns.result.application.service;

import com.ns.common.anotation.UseCase;
import com.ns.common.CreatePlayerCommand;
import com.ns.result.adapter.axon.command.UpdateEloCommand;
import com.ns.result.adapter.axon.query.QueryPlayer;
import com.ns.result.adapter.out.persistence.psql.Player;
import com.ns.result.application.port.in.FindPlayerUseCase;
import com.ns.result.application.port.in.RegisterPlayerUseCase;
import com.ns.result.application.port.in.UpdatePlayerUseCase;
import com.ns.result.application.port.out.SendCommandPort;
import com.ns.result.application.port.out.SendQueryPort;
import com.ns.result.application.port.out.player.FindPlayerPort;
import com.ns.result.application.port.out.player.RegisterPlayerPort;
import com.ns.result.application.port.out.player.UpdatePlayerPort;
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
    public Mono<QueryPlayer> createPlayer(String membershipId) {
        CreatePlayerCommand playerCommand = new CreatePlayerCommand(membershipId);

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
    public Mono<Player> updateElo(String membershipId, Long newElo) { return updatePlayerPort.updatePlayerElo(membershipId, newElo); }

    @Override
    public Flux<Player> findAll() { return findPlayerPort.findAll(); }

    @Override
    public Mono<QueryPlayer> queryToPlayerByMembershipId(String membershipId) {
        return sendQueryPort.sendPlayerQuery(membershipId);
    }
}
