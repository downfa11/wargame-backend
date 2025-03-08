package com.ns.result.usecase;

import com.ns.common.CreatePlayerCommand;
import com.ns.result.adapter.axon.command.UpdateEloCommand;
import com.ns.result.adapter.axon.query.QueryPlayer;
import com.ns.result.application.port.out.player.FindPlayerPort;
import com.ns.result.application.port.out.player.RegisterPlayerPort;
import com.ns.result.application.port.out.player.UpdatePlayerPort;
import com.ns.result.application.port.out.SendCommandPort;
import com.ns.result.application.port.out.SendQueryPort;
import com.ns.result.adapter.out.persistence.psql.Player;
import com.ns.result.application.service.PlayerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    @InjectMocks private PlayerService playerService;

    @Mock private RegisterPlayerPort registerPlayerPort;
    @Mock private UpdatePlayerPort updatePlayerPort;
    @Mock private FindPlayerPort findPlayerPort;
    @Mock private SendCommandPort sendCommandPort;
    @Mock private SendQueryPort sendQueryPort;

    String membershipId = "1001";
    String aggregateIdentifier = "aggregate-123";
    Player player;
    QueryPlayer queryPlayer;

    @BeforeEach
    void init() {
        String code = "23fesj";
        Long elo = 1200L;

        player = Player.builder()
                .membershipId(membershipId)
                .aggregateIdentifier(aggregateIdentifier)
                .code(code)
                .elo(elo)
                .build();

        queryPlayer = QueryPlayer.builder()
                .membershipId(membershipId)
                .code(code)
                .elo(elo)
                .build();
    }

    @Test
    void 플레이어를_생성하는_메서드() {
        // given
        when(sendCommandPort.sendCreatePlayer(any(CreatePlayerCommand.class))).thenReturn(Mono.just(aggregateIdentifier));
        when(registerPlayerPort.registerPlayer(anyString(), anyString())).thenReturn(Mono.just(player));
        when(sendQueryPort.sendPlayerQuery(anyString())).thenReturn(Mono.just(queryPlayer));

        // when
        Mono<QueryPlayer> result = playerService.createPlayer(membershipId);

        // then
        StepVerifier.create(result)
                .expectNext(queryPlayer)
                .verifyComplete();
        verify(sendCommandPort).sendCreatePlayer(any(CreatePlayerCommand.class));
        verify(registerPlayerPort).registerPlayer(membershipId, aggregateIdentifier);
        verify(sendQueryPort).sendPlayerQuery(membershipId);
    }

    @Test
    void 이벤트에_기반한_플레이어의_실력점수_변동_메서드() {
        // given
        String membershipId = "1001";
        String code = "129fjse";
        String aggregateIdentifier = "aggregate";
        Long currentElo = 1250L;
        Long balancedElo = 1300L;

        Player player = Player.builder()
                .membershipId(membershipId)
                .aggregateIdentifier(aggregateIdentifier)
                .code(code)
                .elo(currentElo)
                .build();

        when(findPlayerPort.findByMembershipId(anyString())).thenReturn(Mono.just(player));
        when(sendCommandPort.sendUpdatePlayerElo(any(UpdateEloCommand.class))).thenReturn(Mono.empty());
        when(sendQueryPort.sendPlayerQuery(anyString()))
                .thenReturn(Mono.just(QueryPlayer.builder()
                        .membershipId(membershipId)
                        .code(code)
                        .elo(balancedElo)
                        .build()));

        // when
        Mono<QueryPlayer> result = playerService.updateEloByEvent(membershipId, balancedElo);

        // then
        StepVerifier.create(result)
                .expectNextMatches(queryPlayer -> queryPlayer.getMembershipId().equals(membershipId))
                .verifyComplete();

        verify(sendCommandPort).sendUpdatePlayerElo(any(UpdateEloCommand.class));
        verify(sendQueryPort).sendPlayerQuery(membershipId);
    }

    @Test
    void 사용자의_실력점수를_업데이트하는_메서드() {
        // given
        Long increase = 50L;
        when(updatePlayerPort.updatePlayer(anyString(), anyLong())).thenReturn(Mono.just(player));

        // when
        Mono<Player> result = playerService.updateElo(membershipId, increase);

        // then
        StepVerifier.create(result)
                .expectNext(player)
                .verifyComplete();

        verify(updatePlayerPort).updatePlayer(membershipId, increase);
    }

    @Test
    void 개발_환경에서_사용할_목적의_findAll_메서드() {
        // given
        String membershipId2 = "1002";
        String aggregateIdentifier2 = "aggregateIdentifier";
        String code2 = "code";
        Long elo = 1200L;

        Player player2 = Player.builder()
                .membershipId(membershipId2)
                .aggregateIdentifier(aggregateIdentifier2)
                .code(code2)
                .elo(elo)
                .build();

        when(findPlayerPort.findAll()).thenReturn(Flux.just(player, player2));

        // when
        Flux<Player> result = playerService.findAll();

        // then
        StepVerifier.create(result)
                .expectNext(player)
                .expectNext(player2)
                .verifyComplete();
        verify(findPlayerPort).findAll();
    }

    @Test
    void 이벤트에_기반한__쿼리를_통해서_QueryPlayer_형태로_조회하는_메서드() {
        // given
        String membershipId = "1001";
        String code = "2309js";
        Long elo = 1200L;

        QueryPlayer queryPlayer = QueryPlayer.builder()
                .membershipId(membershipId)
                .code(code)
                .elo(elo)
                .build();

        when(sendQueryPort.sendPlayerQuery(anyString()))
                .thenReturn(Mono.just(queryPlayer));

        // when
        Mono<QueryPlayer> result = playerService.queryToPlayerByMembershipId(membershipId);

        // then
        StepVerifier.create(result)
                .expectNext(queryPlayer)
                .verifyComplete();

        verify(sendQueryPort).sendPlayerQuery(membershipId);
    }
}
