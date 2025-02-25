package com.ns.result;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ns.common.ClientRequest;
import com.ns.common.GameFinishedEvent;
import com.ns.common.task.SubTask;
import com.ns.common.task.Task;
import com.ns.common.task.TaskUseCase;
import com.ns.result.adapter.out.persistence.elasticsearch.Result;
import com.ns.result.application.port.out.cache.FindRedisPort;
import com.ns.result.application.port.out.cache.PushRedisPort;
import com.ns.result.application.port.out.search.FindResultPort;
import com.ns.result.application.port.out.search.RegisterResultPort;
import com.ns.result.application.service.ResultService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ResultServiceTest {

    @InjectMocks private ResultService resultService;

    @Mock private PushRedisPort pushRedisPort;
    @Mock private FindRedisPort findRedisPort;
    @Mock private RegisterResultPort registerResultPort;
    @Mock private FindResultPort findResultPort;

    Result result;

    @BeforeEach
    void init() {
        ClientRequest bluePlayer1 = ClientRequest.builder()
                .membershipId(1L)
                .user_name("BluePlayer1")
                .team("Blue")
                .channel(1)
                .room(1)
                .build();

        ClientRequest redPlayer1 = ClientRequest.builder()
                .membershipId(3L)
                .user_name("RedPlayer1")
                .team("Red")
                .channel(2)
                .room(1)
                .build();

        result = Result.builder()
                .spaceId("dummy-space-id")
                .state("success")
                .channel(1)
                .room(1)
                .winTeam("Blue")
                .loseTeam("Red")
                .blueTeams(List.of(bluePlayer1))
                .redTeams(List.of(redPlayer1))
                .dateTime("2025-02-25T12:00:00")
                .gameDuration(300)
                .build();
    }

    @Test
    void 전적_리스트를_조회하는_메서드() {
        // given
        Result result2 = Result.builder()
                .spaceId("dummy-space-id")
                .state("success")
                .channel(1)
                .room(1)
                .winTeam("Blue")
                .loseTeam("Red")
                .blueTeams(List.of())
                .redTeams(List.of())
                .dateTime("2025-02-25T12:00:00")
                .gameDuration(300)
                .build();

        when(findResultPort.findAll()).thenReturn(Flux.just(result, result2));

        // when
        Flux<Result> resultFlux = resultService.getResultList();

        // then
        StepVerifier.create(resultFlux)
                .expectNext(result)
                .expectNext(result2)
                .verifyComplete();

        verify(findResultPort).findAll();
    }

    @Test
    void 사용자의_이름을_통해서_전적_결과를_조회하는_메서드() {
        // given
        String name = "BluePlayer1";
        int offset = 0;

        when(findResultPort.searchByUserName(anyString(), anyInt())).thenReturn(Flux.just(result));
        when(findRedisPort.findResultInRange(anyString(), anyInt())).thenReturn(Flux.empty());
        when(pushRedisPort.pushResult(anyString(), any())).thenReturn(Mono.just(result));
        // when
        Flux<Result> resultFlux = resultService.getGameResultsByName(name, offset);

        // then
        StepVerifier.create(resultFlux)
                .expectNext(result)
                .verifyComplete();

        verify(findResultPort).searchByUserName(name, offset);
        verify(findRedisPort).findResultInRange(anyString(), anyInt());
    }

    @Test
    void 사용자의_MembershipId을_통해서_전적_결과를_조회하는_메서드() {
        // given
        Long membershipId = 1001L;
        int offset = 0;

        when(findResultPort.searchByMembershipId(anyLong(), anyInt())).thenReturn(Flux.just(result));
        when(findRedisPort.findResultInRange(anyString(), anyInt())).thenReturn(Flux.empty());
        when(pushRedisPort.pushResult(anyString(), any())).thenReturn(Mono.just(result));
        // when
        Flux<Result> resultFlux = resultService.getGameResultsByMembershipId(membershipId, offset);

        // then
        StepVerifier.create(resultFlux)
                .expectNext(result)
                .verifyComplete();

        verify(findResultPort).searchByMembershipId(membershipId, offset);
        verify(findRedisPort).findResultInRange(anyString(), anyInt());
    }

    @Test
    void 게임종료_이벤트_발행시_전적을_저장하는_메서드() {
        // given
        GameFinishedEvent gameFinishedEvent = GameFinishedEvent.builder()
                .spaceId("dummy-space-id")
                .state("success")
                .winTeam("Blue")
                .loseTeam("Red")
                .blueTeams(List.of(createRandomClientRequest(1L, "BluePlayer1")))
                .redTeams(List.of(createRandomClientRequest(2L, "RedPlayer1")))
                .dateTime("2025-02-25")
                .gameDuration(60)
                .build();

        when(registerResultPort.saveResult(any(GameFinishedEvent.class))).thenReturn(Mono.just(result));

        // when
        Mono<Result> resultMono = resultService.saveResult(gameFinishedEvent);

        // then
        StepVerifier.create(resultMono)
                .expectNext(result)
                .verifyComplete();

        verify(registerResultPort).saveResult(gameFinishedEvent);
    }

    @Test
    void 테스트를_위한_더미_전적을_생성하는_메서드() {
        // given
        when(registerResultPort.saveResult(any(GameFinishedEvent.class))).thenReturn(Mono.just(result));

        // when
        Mono<Result> resultMono = resultService.createResultTemp();

        // then
        StepVerifier.create(resultMono)
                .expectNext(result)
                .verifyComplete();
        verify(registerResultPort).saveResult(any(GameFinishedEvent.class));
    }

    @Test
    void 게임종료_이벤트에서_전체_팀에서_사용자들을_리스트로_반환하는_메서드() {
        // given
        GameFinishedEvent gameFinishedEvent = GameFinishedEvent.builder()
                .blueTeams(List.of(createRandomClientRequest(1L, "BluePlayer1")))
                .redTeams(List.of(createRandomClientRequest(2L, "RedPlayer1")))
                .build();

        // when
        List<ClientRequest> allTeams = resultService.getAllTeams(gameFinishedEvent);

        // then
        assertEquals(2, allTeams.size());
    }

    private ClientRequest createRandomClientRequest(Long membershipId, String userName) {
        return ClientRequest.builder()
                .membershipId(membershipId)
                .user_name(userName)
                .team("Blue")
                .build();
    }
}
