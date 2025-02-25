package com.ns.resultquery;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ns.resultquery.adapter.axon.query.CountSumByChamp;
import com.ns.resultquery.adapter.axon.query.CountSumByMembership;
import com.ns.resultquery.application.port.out.FindStatisticsPort;
import com.ns.resultquery.application.service.FindStatisticsService;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class FindStatisticsServiceTest {

    @Mock private FindStatisticsPort findStatisticsPort;

    @InjectMocks private FindStatisticsService findStatisticsService;


    @Test
    void 챔프의_이름으로_해당_챔프의_통계를_조회하는_메서드() {
        // given
        String champName = "testChamp";
        CountSumByChamp expectedResponse = CountSumByChamp.builder()
                .champName(champName)
                .champCount(100L)
                .winCount(200L)
                .loseCount(50L)
                .build();

        when(findStatisticsPort.queryToResultSumByChampName(champName)).thenReturn(Mono.just(expectedResponse));

        // when
        Mono<CountSumByChamp> result = findStatisticsService.findStatisticsByChampion(champName);

        // then
        StepVerifier.create(result)
                .expectNext(expectedResponse)
                .verifyComplete();

        verify(findStatisticsPort, times(1)).queryToResultSumByChampName(champName);
    }

    @Test
    void 사용자의_이름으로_해당_사용자의_통계를_조회하는_메서드() {
        // given
        String userName = "player1";
        CountSumByMembership expectedResponse = CountSumByMembership.builder()
                .username(userName)
                .entireCount(500L)
                .winCount(300L)
                .loseCount(400L)
                .champStatList(Collections.emptyList())
                .build();

        when(findStatisticsPort.queryToResultByUserName(userName)).thenReturn(Mono.just(expectedResponse));

        // when
        Mono<CountSumByMembership> result = findStatisticsService.findStatisticsByUserName(userName);

        // then
        StepVerifier.create(result)
                .expectNext(expectedResponse)
                .verifyComplete();

        verify(findStatisticsPort, times(1)).queryToResultByUserName(userName);
    }
}
