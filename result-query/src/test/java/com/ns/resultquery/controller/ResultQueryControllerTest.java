package com.ns.resultquery.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.ns.resultquery.adapter.axon.query.ChampStat;
import com.ns.resultquery.adapter.axon.query.CountSumByChamp;
import com.ns.resultquery.adapter.axon.query.CountSumByMembership;
import com.ns.resultquery.adapter.in.web.ResultQueryController;
import com.ns.resultquery.application.port.in.FindStatisticsUseCase;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest(ResultQueryController.class)
class ResultQueryControllerTest {

    @Autowired private WebTestClient webTestClient;

    @MockBean private FindStatisticsUseCase findStatisticsUseCase;

    private CountSumByChamp champData;
    private CountSumByMembership membershipData;

    @BeforeEach
    void init() {
        champData = CountSumByChamp.builder()
                .champName("champ")
                .champCount(100L)
                .winCount(50L)
                .loseCount(50L)
                .build();

        membershipData = CountSumByMembership.builder()
                .username("player")
                .entireCount(100L)
                .winCount(50L)
                .loseCount(50L)
                .champStatList(List.of(new ChampStat(1L, "champ", 10L, 7L, 3L, "70.0%")))
                .build();
    }

    @Test
    void 챔프_이름으로_통계를_조회하는_메서드() {
        when(findStatisticsUseCase.findStatisticsByChampion(anyString())).thenReturn(Mono.just(champData));

        webTestClient.get()
                .uri("/statistics/query/champ/" + champData.getChampName())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.champName").isEqualTo(champData.getChampName())
                .jsonPath("$.champCount").isEqualTo(champData.getChampCount())
                .jsonPath("$.winCount").isEqualTo(champData.getWinCount())
                .jsonPath("$.loseCount").isEqualTo(champData.getLoseCount())
                .jsonPath("$.percent").isEqualTo(String.format("%.1f", (double) champData.getWinCount() / champData.getChampCount() * 100) + "%");
    }

    @Test
    void 사용자_이름으로_통계를_조회하는_메서드() {
        when(findStatisticsUseCase.findStatisticsByUserName(anyString())).thenReturn(Mono.just(membershipData));

        webTestClient.get()
                .uri("/statistics/query/user/" + membershipData.getUsername())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.userName").isEqualTo(membershipData.getUsername())
                .jsonPath("$.entireCount").isEqualTo(membershipData.getEntireCount())
                .jsonPath("$.winCount").isEqualTo(membershipData.getWinCount())
                .jsonPath("$.loseCount").isEqualTo(membershipData.getLoseCount())
                .jsonPath("$.percent").isEqualTo(String.format("%.1f", (double) membershipData.getWinCount() / membershipData.getEntireCount() * 100) + "%")
                .jsonPath("$.champStatList[0].champName").isEqualTo(membershipData.getChampStatList().get(0).getChampName())
                .jsonPath("$.champStatList[0].winCount").isEqualTo(membershipData.getChampStatList().get(0).getWinCount());
    }
}