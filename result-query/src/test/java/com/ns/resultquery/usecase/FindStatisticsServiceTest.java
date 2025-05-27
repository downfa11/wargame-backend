package com.ns.resultquery.usecase;

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

        when(findStatisticsPort.queryToResultSumByChampName(champName)).thenReturn(expectedResponse);

        // when
        CountSumByChamp result = findStatisticsService.findStatisticsByChampion(champName);

        // then
        assert result != null;
        assert result.getChampName().equals(expectedResponse.getChampName());
        assert result.getChampCount().equals(expectedResponse.getChampCount());
        assert result.getWinCount().equals(expectedResponse.getWinCount());
        assert result.getLoseCount().equals(expectedResponse.getLoseCount());

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

        when(findStatisticsPort.queryToResultByUserName(userName)).thenReturn(expectedResponse);

        // when
        CountSumByMembership result = findStatisticsService.findStatisticsByUserName(userName);

        // then
        assert result != null;
        assert result.getUsername().equals(expectedResponse.getUsername());
        assert result.getEntireCount().equals(expectedResponse.getEntireCount());
        assert result.getWinCount().equals(expectedResponse.getWinCount());
        assert result.getLoseCount().equals(expectedResponse.getLoseCount());

        verify(findStatisticsPort, times(1)).queryToResultByUserName(userName);
    }
}