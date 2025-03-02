package com.ns.result.controller;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ns.common.ClientRequest;
import com.ns.result.adapter.axon.command.GameFinishedCommand;
import com.ns.result.adapter.axon.query.QueryPlayer;
import com.ns.result.adapter.out.persistence.psql.Player;
import com.ns.result.application.port.in.FindPlayerUseCase;
import com.ns.result.application.port.in.RegisterPlayerUseCase;
import com.ns.result.application.port.in.UpdatePlayerUseCase;
import java.util.List;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@WebFluxTest(PlayerControllerTest.class)
public class PlayerControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean private RegisterPlayerUseCase registerPlayerUseCase;

    @MockBean private UpdatePlayerUseCase updatePlayerUseCase;

    @MockBean private FindPlayerUseCase findPlayerUseCase;

    @MockBean private CommandGateway commandGateway;

    private QueryPlayer queryPlayer;
    private Player player;

    @BeforeEach
    public void init() {
        queryPlayer = QueryPlayer.builder()
                .membershipId("1")
                .elo(1500L)
                .code("")
                .build();

        player = Player.builder()
                .membershipId("1")
                .aggregateIdentifier("aggregateIdentifier")
                .elo(1500L)
                .code("")
                .build();
    }

    @Test
    public void 이벤트에_기반해서_사용자를_등록하는_메서드() {
        // given
        String membershipId = "12345";
        when(registerPlayerUseCase.createPlayer(membershipId)).thenReturn(Mono.just(queryPlayer));

        // when
        webTestClient.post().uri(uriBuilder -> uriBuilder.path("/v1/player/create")
                        .queryParam("membershipId", membershipId)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(QueryPlayer.class)
                .isEqualTo(queryPlayer);
        // then
        verify(registerPlayerUseCase, times(1)).createPlayer(membershipId);
    }

    @Test
    public void 이벤트에_기반해서_사용자의_실력점수를_업데이트하는_메서드() {
        // given
        String membershipId = "12345";
        Long elo = 1600L;
        when(updatePlayerUseCase.updateEloByEvent(membershipId, elo)).thenReturn(Mono.just(queryPlayer));

        // when
        webTestClient.post().uri(uriBuilder -> uriBuilder.path("/v1/player/increase-elo/event")
                        .queryParam("membershipId",membershipId)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(QueryPlayer.class)
                .isEqualTo(queryPlayer);
        // then
        verify(updatePlayerUseCase, times(1)).updateEloByEvent(membershipId, elo);
    }

    @Test
    public void 테스트를_위해서_게임_룸을_생성하는_메서드() {
        // given
        String state = "success";
        GameFinishedCommand command = GameFinishedCommand.builder()
                .spaceId("12345")
                .state(state)
                .channel(1)
                .room(1)
                .winTeam("blue")
                .loseTeam("red")
                .blueTeams(List.of(new ClientRequest(), new ClientRequest()))
                .redTeams(List.of(new ClientRequest(), new ClientRequest()))
                .dateTime("2025-02-01T12:00:00Z")
                .gameDuration(120)
                .build();

        // when
        webTestClient.get().uri(uriBuilder -> uriBuilder.path("/v1/player/test/room")
                        .queryParam("state", state)
                        .build())
                .exchange()
                .expectStatus()
                .isOk();
        // then
        verify(commandGateway, times(1)).send(any(GameFinishedCommand.class));
    }

    @Test
    public void 모든_플레이어_목록을_조회하는_메서드() {
        // given
        Flux<Player> playersFlux = Flux.just(player);
        when(findPlayerUseCase.findAll()).thenReturn(playersFlux);

        // when
        webTestClient.get().uri("/v1/player/playerList")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Player.class)
                .hasSize(1)
                .contains(player);
        // then
        verify(findPlayerUseCase, times(1)).findAll();
    }

    @Test
    public void membershipId를_통해서_플레이어를_조회하는_메서드() {
        // given
        String membershipId = "12345";
        when(findPlayerUseCase.queryToPlayerByMembershipId(membershipId)).thenReturn(Mono.just(queryPlayer));

        // when
        webTestClient.get().uri("/v1/player/player/{membershipId}", membershipId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(QueryPlayer.class)
                .isEqualTo(queryPlayer);
        // then
        verify(findPlayerUseCase, times(1)).queryToPlayerByMembershipId(membershipId);
    }
}
