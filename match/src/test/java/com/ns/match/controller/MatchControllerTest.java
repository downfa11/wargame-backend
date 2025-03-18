package com.ns.match.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ns.common.MessageEntity;
import com.ns.match.adapter.in.web.MatchController;
import com.ns.match.adapter.in.web.MatchRequest;
import com.ns.match.adapter.out.RedisMatchAdapter.MatchStatus;
import com.ns.match.application.port.in.CancleMatchQueueUseCase;
import com.ns.match.application.port.in.GetMatchQueueUseCase;
import com.ns.match.application.port.in.IntegrationTestMatchUseCase;
import com.ns.match.application.port.in.RegisterMatchQueueUseCase;
import com.ns.match.application.service.MatchResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

@WebFluxTest(MatchController.class)
public class MatchControllerTest {

    @Autowired private WebTestClient webTestClient;

    @MockBean private RegisterMatchQueueUseCase registerMatchQueueUseCase;
    @MockBean private CancleMatchQueueUseCase cancleMatchQueueUseCase;
    @MockBean private GetMatchQueueUseCase getMatchQueueUseCase;
    @MockBean private IntegrationTestMatchUseCase integrationTestMatchUseCase;

    @Test
    public void 매칭_큐에_사용자를_등록하는_메서드() {
        // given
        MatchRequest matchRequest = new MatchRequest(1L);

        when(registerMatchQueueUseCase.registerMatchQueue(any(), any())).thenReturn(Mono.just("success"));

        // when
        webTestClient.post()
                .uri("/game/match")
                .bodyValue(matchRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(MessageEntity.class)
                .consumeWith(response -> {
                    MessageEntity body = response.getResponseBody();
                    assert body != null;
                    assert "Success".equals(body.getMessage());
                });
    }

    @Test
    public void 매칭_큐에_등록된_사용자를_취소하는_메서드() {
        // given
        MatchRequest matchRequest = new MatchRequest(1L);
        when(cancleMatchQueueUseCase.cancelMatchQueue(any())).thenReturn(Mono.empty());

        // when
        webTestClient.post()
                .uri("/game/match/cancel")
                .bodyValue(matchRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(MessageEntity.class)
                .consumeWith(response -> {
                    MessageEntity body = response.getResponseBody();
                    assert body != null;
                    assert "Success".equals(body.getMessage());
                    assert "All queues have been deleted.".equals(body.getResult());
                });
    }

    @Test
    public void 매칭_현황을_확인하는_메서드() {
        // given
        Long memberId = 1L;
        when(getMatchQueueUseCase.getMatchResponse(any())).thenReturn(Mono.just(Tuples.of(MatchStatus.MATCH_FOUND, MatchResponse.builder().build())));

        // when
        webTestClient.get()
                .uri("/game/match/rank/{memberId}", memberId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(MessageEntity.class)
                .consumeWith(response -> {
                    MessageEntity body = response.getResponseBody();
                    assert body != null;
                    assert "Success".equals(body.getMessage());
                    assert "found".equals(body.getResult());
                });
    }

    @Test
    public void 통합_테스트를_위한_API_메서드_검증() {
        // given
        int threads = 10;
        int requests = 5;
        when(integrationTestMatchUseCase.requestIntegrationTest(any(), any())).thenReturn(Mono.just((long)requests));

        // when
        webTestClient.post().uri(uriBuilder -> uriBuilder.path("/game/test/integration")
                .queryParam("threads",String.valueOf(threads))
                .queryParam("requests",String.valueOf(requests))
                .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(MessageEntity.class)
                .consumeWith(response -> {
                    MessageEntity body = response.getResponseBody();
                    assert body != null;
                    assert "success".equals(body.getMessage());
                });
    }
}

