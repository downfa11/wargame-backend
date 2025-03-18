package com.ns.resultquery.adapter;

import static org.mockito.Mockito.*;

import com.ns.resultquery.adapter.axon.QueryResultSumByChampName;
import com.ns.resultquery.adapter.axon.QueryResultSumByUserName;
import com.ns.resultquery.adapter.axon.query.ChampStat;
import com.ns.resultquery.adapter.axon.query.CountSumByChamp;
import com.ns.resultquery.adapter.axon.query.CountSumByMembership;
import com.ns.resultquery.adapter.out.persistence.FindStatisticsAdapter;
import java.util.List;

import java.util.concurrent.CompletableFuture;
import org.axonframework.queryhandling.QueryGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class FindStatisticsAdapterTest {
    private CountSumByChamp champData;
    private CountSumByMembership membershipData;

    @Mock QueryGateway queryGateway;
    @InjectMocks FindStatisticsAdapter findStatisticsAdapter;


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
    void 챔프의_이름으로_통계_쿼리를_전달하는_메서드() {
        // given
        String champName = "champ";
        when(queryGateway.query(any(QueryResultSumByChampName.class), eq(CountSumByChamp.class)))
                .thenReturn(CompletableFuture.completedFuture(champData));

        // when
        Mono<CountSumByChamp> result = findStatisticsAdapter.queryToResultSumByChampName(champName);

        // then
        StepVerifier.create(result)
                .expectNext(champData)
                .verifyComplete();
    }

    @Test
    void 사용자의_이름으로_통계_쿼리를_전달하는_메서드() {
        // given
        String userName = "player";
        when(queryGateway.query(any(QueryResultSumByUserName.class), eq(CountSumByMembership.class)))
                .thenReturn(CompletableFuture.completedFuture(membershipData));
        // when
        Mono<CountSumByMembership> result = findStatisticsAdapter.queryToResultByUserName(userName);

        // then
        StepVerifier.create(result)
                .expectNext(membershipData)
                .verifyComplete();
    }
}
