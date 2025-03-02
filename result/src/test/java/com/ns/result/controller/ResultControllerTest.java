package com.ns.result.controller;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ns.common.ClientRequest;
import com.ns.result.adapter.out.persistence.elasticsearch.Result;
import com.ns.result.application.port.in.FindResultUseCase;
import com.ns.result.application.port.in.RegisterResultUseCase;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@WebFluxTest(ResultControllerTest.class)
public class ResultControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean private RegisterResultUseCase registerResultUseCase;

    @MockBean private FindResultUseCase findResultUseCase;

    private Result result;

    @BeforeEach
    public void init() {
        result = Result.builder()
                .spaceId("12345")
                .state("success")
                .channel(1)
                .room(101)
                .winTeam("blue")
                .loseTeam("red")
                .blueTeams(List.of(ClientRequest.builder().build(), ClientRequest.builder().build()))
                .redTeams(List.of(ClientRequest.builder().build(), ClientRequest.builder().build()))
                .dateTime("2025-02-01T12:00:00Z")
                .gameDuration(120)
                .build();
    }

    @Test
    public void 모든_전적_목록을_조회하는_메서드() {
        // given
        Flux<Result> resultFlux = Flux.just(result);
        when(findResultUseCase.getResultList()).thenReturn(resultFlux);

        // when
        webTestClient.get().uri("/v1/result/list")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Result.class)
                .hasSize(1)
                .contains(result);
        // then
        verify(findResultUseCase, times(1)).getResultList();
    }

    @Test
    public void 사용자의_이름으로_전적_결과를_조회하는_메서드() {
        // given
        String name = "Test Name";
        int offset = 0;
        Flux<Result> resultFlux = Flux.just(result);
        when(findResultUseCase.getGameResultsByName(name, offset)).thenReturn(resultFlux);

        // when
        webTestClient.get().uri(uriBuilder -> uriBuilder.path("/v1/result/search/name/{name}")
                        .queryParam("offset", offset)
                        .build(name))
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Result.class)
                .hasSize(1)
                .contains(result);
        // then
        verify(findResultUseCase, times(1)).getGameResultsByName(name, offset);
    }

    @Test
    public void membershipId로_전적_결과를_조회하는_메서드() {
        // given
        Long membershipId = 123L;
        int offset = 0;
        Flux<Result> resultFlux = Flux.just(result);
        when(findResultUseCase.getGameResultsByMembershipId(membershipId, offset)).thenReturn(resultFlux);

        // when
        webTestClient.get().uri(uriBuilder -> uriBuilder.path("/v1/result/search/id/{membershipId}")
                        .queryParam("offset", offset)
                        .build(membershipId))
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Result.class)
                .hasSize(1)
                .contains(result);
        // then
        verify(findResultUseCase, times(1)).getGameResultsByMembershipId(membershipId, offset);
    }

    @Test
    public void 테스트를_위해_더미_전적을_생성하는_메서드() {
        // given
        Mono<Result> resultMono = Mono.just(result);
        when(registerResultUseCase.createResultTemp()).thenReturn(resultMono);

        // when
        webTestClient.post().uri("/v1/result/temp")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Result.class)
                .isEqualTo(result);
        // then
        verify(registerResultUseCase, times(1)).createResultTemp();
    }
}
