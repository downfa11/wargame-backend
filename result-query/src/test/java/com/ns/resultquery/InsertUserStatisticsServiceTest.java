package com.ns.resultquery;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ns.resultquery.application.port.out.InsertChampStatisticsPort;
import com.ns.resultquery.application.port.out.InsertUserStatisticsPort;
import com.ns.resultquery.application.service.InsertUserStatisticsService;
import com.ns.resultquery.domain.dto.InsertResultCountDto;
import com.ns.resultquery.domain.dto.MembershipResultEventDto;
import com.ns.resultquery.domain.dto.ResultEventDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class InsertUserStatisticsServiceTest {

    @Mock private InsertUserStatisticsPort insertUserStatisticsPort;
    @Mock private InsertChampStatisticsPort insertChampStatisticsPort;

    @InjectMocks private InsertUserStatisticsService insertUserStatisticsService;


    @Test
    void 게임종료_이벤트_발행시_각_챔프별_통계를_업데이트하는_메서드() {
        // given
        ResultEventDto eventDto = ResultEventDto.builder()
                .champIndex(1L)
                .champName("testChamp")
                .resultCount(10L)
                .winCount(6L)
                .loseCount(4L)
                .build();

        when(insertChampStatisticsPort.insertResultCountIncreaseEventByChampName(
                eventDto.getChampIndex(),
                eventDto.getChampName(),
                eventDto.getResultCount(),
                eventDto.getWinCount(),
                eventDto.getLoseCount()))
                .thenReturn(Mono.empty());

        // when
        Mono<Void> result = insertUserStatisticsService.insertResultCountIncreaseEventByChampName(eventDto);

        // then
        StepVerifier.create(result).verifyComplete();

        verify(insertChampStatisticsPort, times(1))
                .insertResultCountIncreaseEventByChampName(
                        eventDto.getChampIndex(),
                        eventDto.getChampName(),
                        eventDto.getResultCount(),
                        eventDto.getWinCount(),
                        eventDto.getLoseCount());
    }

    @Test
    void 게임종료_이벤트_발행시_사용자별_통계를_업데이트하는_메서드() {
        // given
        MembershipResultEventDto eventDto = MembershipResultEventDto.builder()
                .membershipId(1001L)
                .userName("player1")
                .champIndex(1L)
                .champName("testChamp")
                .resultCount(10L)
                .winCount(6L)
                .loseCount(4L)
                .build();

        InsertResultCountDto expectedDto = InsertResultCountDto.builder()
                .champIndex(eventDto.getChampIndex())
                .champName(eventDto.getChampName())
                .champResult(eventDto.getResultCount())
                .champWin(eventDto.getWinCount())
                .champLose(eventDto.getLoseCount())
                .resultCount(eventDto.getResultCount())
                .winCount(eventDto.getWinCount())
                .loseCount(eventDto.getLoseCount())
                .build();

        when(insertUserStatisticsPort.insertResultCountIncreaseEventByUserName(eventDto.getMembershipId(), eventDto.getUserName(), expectedDto))
                .thenReturn(Mono.empty());

        // when
        Mono<Void> result = insertUserStatisticsService.insertResultCountIncreaseEventByUserName(eventDto);

        // then
        StepVerifier.create(result).verifyComplete();

        verify(insertUserStatisticsPort, times(1))
                .insertResultCountIncreaseEventByUserName(eventDto.getMembershipId(), eventDto.getUserName(), expectedDto);
    }
}
