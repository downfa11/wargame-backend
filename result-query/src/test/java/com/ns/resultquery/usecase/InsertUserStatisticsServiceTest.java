package com.ns.resultquery.usecase;

import com.ns.resultquery.application.port.out.InsertChampStatisticsPort;
import com.ns.resultquery.application.port.out.InsertUserStatisticsPort;
import com.ns.resultquery.application.service.InsertUserStatisticsService;
import com.ns.resultquery.domain.dto.InsertResultCountDto;
import com.ns.resultquery.domain.dto.MembershipResultEventDto;
import com.ns.resultquery.domain.dto.ResultEventDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InsertUserStatisticsServiceTest {

    @Mock private InsertUserStatisticsPort insertUserStatisticsPort;
    @Mock private InsertChampStatisticsPort insertChampStatisticsPort;

    @InjectMocks private InsertUserStatisticsService insertUserStatisticsService;

    MembershipResultEventDto eventDto;

    InsertResultCountDto expectedDto;

    @BeforeEach
    void init(){
        eventDto = MembershipResultEventDto.builder()
                .membershipId(1001L)
                .userName("player1")
                .champIndex(1L)
                .champName("testChamp")
                .resultCount(10L)
                .winCount(6L)
                .loseCount(4L)
                .build();

        expectedDto = InsertResultCountDto.builder()
                .champIndex(eventDto.getChampIndex())
                .champName(eventDto.getChampName())
                .champResult(eventDto.getResultCount())
                .champWin(eventDto.getWinCount())
                .champLose(eventDto.getLoseCount())
                .resultCount(eventDto.getResultCount())
                .winCount(eventDto.getWinCount())
                .loseCount(eventDto.getLoseCount())
                .build();
    }

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

        doNothing().when(insertChampStatisticsPort).insertResultCountIncreaseEventByChampName(
                eventDto.getChampIndex(),
                eventDto.getChampName(),
                eventDto.getResultCount(),
                eventDto.getWinCount(),
                eventDto.getLoseCount());

        // when
        insertUserStatisticsService.insertResultCountIncreaseEventByChampName(eventDto);

        // then
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
        doNothing().when(insertUserStatisticsPort)
                .insertResultCountIncreaseEventByUserName(anyLong(), anyString(), any());

        // when
        insertUserStatisticsService.insertResultCountIncreaseEventByUserName(eventDto);

        // then
        verify(insertUserStatisticsPort, times(1))
                .insertResultCountIncreaseEventByUserName(eq(eventDto.getMembershipId()), eq(eventDto.getUserName()), any(InsertResultCountDto.class));
    }
}