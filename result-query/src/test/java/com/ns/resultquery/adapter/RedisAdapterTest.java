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
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.ValueOperations;

import static org.mockito.Mockito.*;

import java.util.List;

@ExtendWith(MockitoExtension.class)
public class RedisAdapterTest {

    @Mock private RedisOperations<String, CountSumByChamp> champRedisTemplate;
    @Mock private RedisOperations<String, CountSumByMembership> membershipRedisTemplate;

    @Mock private ValueOperations<String, CountSumByChamp> champValueOps;
    @Mock private ValueOperations<String, CountSumByMembership> membershipValueOps;

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
        when(champValueOps.get(key)).thenReturn(countSumByChamp);

        // when
        CountSumByChamp result = redisAdapter.findCountSumByChampInRange(key);

        // then
        assert result != null;
        assert result.getChampName().equals(countSumByChamp.getChampName());
        assert result.getChampCount().equals(countSumByChamp.getChampCount());
        assert result.getWinCount().equals(countSumByChamp.getWinCount());
        assert result.getLoseCount().equals(countSumByChamp.getLoseCount());

        verify(champRedisTemplate, times(1)).opsForValue();
        verify(champRedisTemplate.opsForValue(), times(1)).get(key);
    }

    @Test
    void 전적_결과를_캐싱_등록하는_메서드() {
        // given
        String key = "testKey";
        when(champRedisTemplate.opsForValue()).thenReturn(champValueOps);

        // when
        CountSumByChamp pushedResult = redisAdapter.pushCountSumByChamp(key, countSumByChamp);

        // then
        assert pushedResult != null;
        assert pushedResult.getChampName().equals(countSumByChamp.getChampName());
        assert pushedResult.getChampCount().equals(countSumByChamp.getChampCount());
        assert pushedResult.getWinCount().equals(countSumByChamp.getWinCount());
        assert pushedResult.getLoseCount().equals(countSumByChamp.getLoseCount());

        verify(champRedisTemplate, times(1)).opsForValue();
        verify(champRedisTemplate.opsForValue(), times(1)).set(key, countSumByChamp);
    }

    @Test
    void 회원_전적_데이터를_캐싱_조회하는_메서드() {
        // given
        String key = "testMembershipKey";
        when(membershipRedisTemplate.opsForValue()).thenReturn(membershipValueOps);
        when(membershipValueOps.get(key)).thenReturn(countSumByMembership);

        // when
        CountSumByMembership result = redisAdapter.findCountSumByMembershipInRange(key);

        // then
        assert result != null;
        assert result.getUsername().equals(countSumByMembership.getUsername());
        assert result.getEntireCount().equals(countSumByMembership.getEntireCount());
        assert result.getWinCount().equals(countSumByMembership.getWinCount());
        assert result.getLoseCount().equals(countSumByMembership.getLoseCount());

        verify(membershipRedisTemplate, times(1)).opsForValue();
        verify(membershipRedisTemplate.opsForValue(), times(1)).get(key);
    }

    @Test
    void 회원_전적_결과를_캐싱_등록하는_메서드() {
        // given
        String key = "testMembershipKey";
        when(membershipRedisTemplate.opsForValue()).thenReturn(membershipValueOps);

        // when
        CountSumByMembership pushedResult = redisAdapter.pushCountSumByMembership(key, countSumByMembership);

        // then
        assert pushedResult != null;
        assert pushedResult.getUsername().equals(countSumByMembership.getUsername());
        assert pushedResult.getEntireCount().equals(countSumByMembership.getEntireCount());
        assert pushedResult.getWinCount().equals(countSumByMembership.getWinCount());
        assert pushedResult.getLoseCount().equals(countSumByMembership.getLoseCount());

        verify(membershipRedisTemplate, times(1)).opsForValue();
        verify(membershipRedisTemplate.opsForValue(), times(1)).set(key, countSumByMembership);
    }
}
