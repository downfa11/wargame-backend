package com.ns.result.controller;


import static reactor.core.publisher.Mono.when;

import com.ns.common.ClientRequest;
import com.ns.common.TimelineData;
import com.ns.result.adapter.in.web.MatchDto;
import com.ns.result.adapter.in.web.ResultController;
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

@WebFluxTest(ResultController.class)
public class ResultControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean private RegisterResultUseCase registerResultUseCase;
    @MockBean private FindResultUseCase findResultUseCase;

    private Result result;
    private MatchDto matchDto;

    @BeforeEach
    public void init() {
        result = Result.builder()
                .spaceId("12345")
                .state("success")
                .channel(1)
                .room(101)
                .winTeam("blue")
                .loseTeam("red")
                .blueTeams(List.of(ClientRequest.builder().nickname("A").build()))
                .redTeams(List.of(ClientRequest.builder().nickname("B").build()))
                .dateTime("2025-02-01T12:00:00Z")
                .gameDuration(120)
                .timelineData(List.of(TimelineData.builder().time(1).blueGold(1000).redGold(900).build()))
                .build();

        matchDto = MatchDto.from(result);
    }

    @Test
    void 모든_전적_목록을_조회하는_메서드() {
        when(findResultUseCase.getResultList()).thenReturn(Flux.just(result));

        webTestClient.get().uri("/v1/result/list")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(MatchDto.class)
                .hasSize(1)
                .contains(matchDto);

        verify(findResultUseCase, times(1)).getResultList();
    }

    @Test
    void 사용자의_이름으로_전적_결과를_조회하는_메서드() {
        String name = "Test Name";
        int offset = 0;
        when(findResultUseCase.getGameResultsByName(name, offset)).thenReturn(Flux.just(result));

        webTestClient.get().uri(uriBuilder -> uriBuilder.path("/v1/result/search/name/{name}")
                        .queryParam("offset", offset)
                        .build(name))
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(MatchDto.class)
                .hasSize(1)
                .contains(matchDto);

        verify(findResultUseCase, times(1)).getGameResultsByName(name, offset);
    }

    @Test
    void membershipId로_전적_결과를_조회하는_메서드() {
        Long membershipId = 123L;
        int offset = 0;
        when(findResultUseCase.getGameResultsByMembershipId(membershipId, offset)).thenReturn(Flux.just(result));

        webTestClient.get().uri(uriBuilder -> uriBuilder.path("/v1/result/search/id/{membershipId}")
                        .queryParam("offset", offset)
                        .build(membershipId))
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(MatchDto.class)
                .hasSize(1)
                .contains(matchDto);

        verify(findResultUseCase, times(1)).getGameResultsByMembershipId(membershipId, offset);
    }

    @Test
    void 테스트를_위해_더미_전적을_생성하는_메서드() {
        when(registerResultUseCase.createResultTemp()).thenReturn(Mono.just(result));

        webTestClient.post().uri("/v1/result/temp")
                .exchange()
                .expectStatus().isOk()
                .expectBody(MatchDto.class)
                .isEqualTo(matchDto);

        verify(registerResultUseCase, times(1)).createResultTemp();
    }
}
