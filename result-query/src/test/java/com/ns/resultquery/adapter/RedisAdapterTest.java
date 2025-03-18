package com.ns.resultquery.adapter;

import com.ns.resultquery.adapter.axon.query.ChampStat;
import com.ns.resultquery.adapter.out.RedisAdapter;
import com.ns.resultquery.adapter.axon.query.CountSumByChamp;
import com.ns.resultquery.adapter.axon.query.CountSumByMembership;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RedisAdapterTest {

    @Mock private ReactiveRedisOperations<String, CountSumByChamp> champRedisTemplate;
    @Mock private ReactiveRedisOperations<String, CountSumByMembership> membershipRedisTemplate;

    @Mock private ReactiveValueOperations<String, CountSumByChamp> champValueOps;
    @Mock private ReactiveValueOperations<String, CountSumByMembership> membershipValueOps;

    @InjectMocks private RedisAdapter redisAdapter;

    private CountSumByChamp countSumByChamp;
    private CountSumByMembership countSumByMembership;

    @BeforeEach
    void init() {
        countSumByChamp = CountSumByChamp.builder()
                .champName("champ1")
                .champCount(100L)
                .winCount(60L)
                .loseCount(40L)
                .build();

        List<ChampStat> champStatList = List.of(
                ChampStat.builder()
                        .champName("champ1")
                        .winCount(30L)
                        .loseCount(10L)
                        .build(),
                ChampStat.builder()
                        .champName("champ2")
                        .winCount(20L)
                        .loseCount(10L)
                        .build()
        );

        countSumByMembership = CountSumByMembership.builder()
                .username("Player1")
                .entireCount(100L)
                .winCount(60L)
                .loseCount(40L)
                .champStatList(champStatList)
                .build();
    }

    @Test
    void 전적_데이터를_캐싱_조회하는_메서드() {
        // given
        String key = "testKey";
        when(champRedisTemplate.opsForValue()).thenReturn(champValueOps);
        when(champValueOps.get(key)).thenReturn(Mono.just(countSumByChamp));

        // when
        Mono<CountSumByChamp> resultMono = redisAdapter.findCountSumByChampInRange(key);

        // then
        StepVerifier.create(resultMono)
                .expectNext(countSumByChamp)
                .verifyComplete();

        verify(champRedisTemplate, times(1)).opsForValue();
        verify(champRedisTemplate.opsForValue(), times(1)).get(key);
    }

    @Test
    void 전적_결과를_캐싱_등록하는_메서드() {
        // given
        String key = "testKey";
        when(champRedisTemplate.opsForValue()).thenReturn(champValueOps);
        when(champValueOps.set(key, countSumByChamp)).thenReturn(Mono.just(true));

        // when
        Mono<CountSumByChamp> pushedResultMono = redisAdapter.pushCountSumByChamp(key, countSumByChamp);

        // then
        StepVerifier.create(pushedResultMono)
                .expectNext(countSumByChamp)
                .verifyComplete();

        verify(champRedisTemplate, times(1)).opsForValue();
        verify(champRedisTemplate.opsForValue(), times(1)).set(key, countSumByChamp);
    }

    @Test
    void 회원_전적_데이터를_캐싱_조회하는_메서드() {
        // given
        String key = "testMembershipKey";
        when(membershipRedisTemplate.opsForValue()).thenReturn(membershipValueOps);
        when(membershipValueOps.get(key)).thenReturn(Mono.just(countSumByMembership));

        // when
        Mono<CountSumByMembership> resultMono = redisAdapter.findCountSumByMembershipInRange(key);

        // then
        StepVerifier.create(resultMono)
                .expectNext(countSumByMembership)
                .verifyComplete();

        verify(membershipRedisTemplate, times(1)).opsForValue();
        verify(membershipRedisTemplate.opsForValue(), times(1)).get(key);
    }

    @Test
    void 회원_전적_결과를_캐싱_등록하는_메서드() {
        // given
        String key = "testMembershipKey";
        when(membershipRedisTemplate.opsForValue()).thenReturn(membershipValueOps);
        when(membershipValueOps.set(key, countSumByMembership)).thenReturn(Mono.just(true));

        // when
        Mono<CountSumByMembership> pushedResultMono = redisAdapter.pushCountSumByMembership(key, countSumByMembership);

        // then
        StepVerifier.create(pushedResultMono)
                .expectNext(countSumByMembership)
                .verifyComplete();

        verify(membershipRedisTemplate, times(1)).opsForValue();
        verify(membershipRedisTemplate.opsForValue(), times(1)).set(key, countSumByMembership);
    }
}