package com.ns.result.application.service;

import com.ns.result.adapter.axon.command.CreatePlayerCommand;
import com.ns.result.adapter.axon.command.UpdateEloCommand;
import com.ns.result.adapter.axon.query.FindPlayerAggregateQuery;
import com.ns.result.adapter.axon.query.QueryPlayer;
import com.ns.result.adapter.out.persistence.psql.Player;
import com.ns.result.adapter.out.persistence.psql.PlayerR2dbcRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerService {
    private final PlayerR2dbcRepository playerRepository;
    private final CommandGateway commandGateway;
    private final QueryGateway queryGateway;

    public Mono<Void> createPlayerByEvent(String membershipId) {
        CreatePlayerCommand playerCommand = new CreatePlayerCommand(membershipId);
        log.info("CreatePlayerCommand를 전달.");
        return Mono.fromFuture(() -> commandGateway.send(playerCommand))
                .doOnSuccess(result -> create(membershipId, (String) result).subscribe())
                .doOnError(throwable -> log.error("createMemberByEvent throwable : ", throwable))
                .then();
    }

    public Mono<Player> create(String membershipId, String aggregateIdentifier) {
        Player player = Player.builder()
                .membershipId(membershipId)
                .aggregateIdentifier(aggregateIdentifier)
                .elo(2000L)
                .code("").build();

        return playerRepository.save(player);
    }

    public Mono<QueryPlayer> updateEloByEvent(String membershipId, Long elo) {
        return playerRepository.findById(Long.parseLong(membershipId))
                .flatMap(user -> {
                    String memberAggregateIdentifier = user.getAggregateIdentifier();
                    UpdateEloCommand axonCommand = new UpdateEloCommand(memberAggregateIdentifier, membershipId, elo);

                    return Mono.fromFuture(() -> commandGateway.send(axonCommand))
                            .then(Mono.defer(() -> queryToPlayerByMembershipId(membershipId)))
                            .doOnError(throwable -> log.error("modifyMemberEloByEvent throwable : ", throwable));
                });
    }

    public Mono<Player> updateElo(String membershipId, Long increase) {
        return playerRepository.findByMembershipId(membershipId)
                .flatMap(u -> {
                    Long newElo = u.getElo() + increase;
                    u.setElo(newElo);
                    return playerRepository.save(u);
                });
    }

    public Mono<Player> findByMembershipId(String membershipId) {
        return playerRepository.findByMembershipId(membershipId);
    }

    public Flux<Player> findAll() {
        return playerRepository.findAll();
    }

    public Mono<QueryPlayer> updateEloBySaga(String membershipId, Long elo) {
        return playerRepository.findByMembershipId(membershipId)
                .flatMap(player -> {
                    String aggregateIdentifier = player.getAggregateIdentifier();
                    UpdateEloCommand command = new UpdateEloCommand(aggregateIdentifier, membershipId, elo);

                    return Mono.fromFuture(() -> commandGateway.send(command))
                            .doOnError(throwable -> log.error("Failed to start Elo Update Saga", throwable))
                            .then(Mono.defer(() -> queryToPlayerByMembershipId(membershipId)));
                });
    }

    public Mono<QueryPlayer> queryToPlayerByMembershipId(String membershipId) {
        return Mono.fromFuture(() -> queryGateway.query(
                new FindPlayerAggregateQuery(membershipId), QueryPlayer.class));
    }
}
