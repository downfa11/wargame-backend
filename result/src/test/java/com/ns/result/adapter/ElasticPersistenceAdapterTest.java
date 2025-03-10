package com.ns.result.adapter;

import com.ns.common.ClientRequest;
import com.ns.common.GameFinishedEvent;
import com.ns.result.adapter.out.persistence.elasticsearch.Result;
import com.ns.result.adapter.out.persistence.elasticsearch.ElasticPersistenceAdapter;
import com.ns.result.adapter.out.persistence.elasticsearch.ResultRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ElasticPersistenceAdapterTest {

    @Mock private ResultRepository resultRepository;
    @InjectMocks private ElasticPersistenceAdapter elasticPersistenceAdapter;

    private GameFinishedEvent gameFinishedEvent;
    private Result result;

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

        gameFinishedEvent = new GameFinishedEvent();
    }

    @Test
    void 전적을_기록하는_메서드() {
        // given
        when(resultRepository.save(any())).thenReturn(Mono.just(result));

        // when
        Mono<Result> savedResult = elasticPersistenceAdapter.saveResult(gameFinishedEvent);

        // then
        StepVerifier.create(savedResult)
                .expectNext(result)
                .verifyComplete();

        verify(resultRepository, times(1)).save(any());
    }

    @Test
    void 모든_전적_데이터를_조회하는_메서드() {
        // given
        when(resultRepository.findAll()).thenReturn(Flux.just(result));

        // when
        Flux<Result> results = elasticPersistenceAdapter.findAll();

        // then
        StepVerifier.create(results)
                .expectNext(result)
                .verifyComplete();

        verify(resultRepository, times(1)).findAll();
    }

    @Test
    void 사용자의_이름으로_전적을_조회하는_메서드() {
        // given
        String name = "blue";
        int offset = 0;
        when(resultRepository.searchByUserName(name, 30, offset)).thenReturn(Flux.just(result));

        // when
        Flux<Result> results = elasticPersistenceAdapter.searchByUserName(name, offset);

        // then
        StepVerifier.create(results)
                .expectNext(result)
                .verifyComplete();

        verify(resultRepository, times(1)).searchByUserName(name, 30, offset);
    }

    @Test
    void membershipId로_전적을_조회하는_메서드() {
        // given
        Long membershipId = 123L;
        int offset = 0;
        when(resultRepository.searchByMembershipId(membershipId, 30, offset)).thenReturn(Flux.just(result));

        // when
        Flux<Result> results = elasticPersistenceAdapter.searchByMembershipId(membershipId, offset);

        // then
        StepVerifier.create(results)
                .expectNext(result)
                .verifyComplete();

        verify(resultRepository, times(1)).searchByMembershipId(membershipId, 30, offset);
    }
}

