package com.ns.player.usecase;

import com.ns.common.CreatePlayerCommand;
import com.ns.player.adapter.axon.command.UpdateEloCommand;
import com.ns.player.adapter.axon.query.QueryPlayer;
import com.ns.player.application.port.out.SendCommandPort;
import com.ns.player.application.port.out.SendQueryPort;
import com.ns.player.application.service.PlayerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    @InjectMocks private PlayerService playerService;

    @Mock private SendCommandPort sendCommandPort;
    @Mock private SendQueryPort sendQueryPort;

    String nickname = "user";
    String membershipId = "1001";
    String aggregateIdentifier = "aggregate-123";
    QueryPlayer queryPlayer;

    @BeforeEach
    void init() {
        String code = "23fesj";
        Long elo = 1200L;

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
        when(sendQueryPort.sendPlayerQuery(anyString())).thenReturn(Mono.just(queryPlayer));

        // when
        Mono<QueryPlayer> result = playerService.createPlayer(membershipId, nickname);

        // then
        StepVerifier.create(result)
                .expectNext(queryPlayer)
                .verifyComplete();
        verify(sendCommandPort).sendCreatePlayer(any(CreatePlayerCommand.class));
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
