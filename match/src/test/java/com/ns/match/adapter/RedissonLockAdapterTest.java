package com.ns.match.adapter;


import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.ns.match.adapter.out.RedissonLockAdapter;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.redisson.api.RLockReactive;
import org.redisson.api.RedissonReactiveClient;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

@ExtendWith(SpringExtension.class)
class RedissonLockAdapterTest {

    @Mock private RedissonReactiveClient redissonReactiveClient;
    @Mock private RLockReactive rLockReactive;

    private RedissonLockAdapter redissonLockAdapter;

    @BeforeEach
    void init() {
        redissonLockAdapter = new RedissonLockAdapter(redissonReactiveClient);
    }

    @Test
    void 락을_점유하는_메서드() {
        // given
        when(redissonReactiveClient.getLock("lockKey")).thenReturn(rLockReactive);
        when(rLockReactive.tryLock(3, 3, TimeUnit.SECONDS)).thenReturn(Mono.just(true));

        // when
        Mono<Boolean> result = redissonLockAdapter.acquireLock(rLockReactive);

        // then
        assertTrue(result.block());
        verify(rLockReactive, times(1)).tryLock(3, 3, TimeUnit.SECONDS);
    }

    @Test
    void 락을_점유하는_메서드_실패한_경우() {
        // given
        when(redissonReactiveClient.getLock("lockKey")).thenReturn(rLockReactive);
        when(rLockReactive.tryLock(3, 3, TimeUnit.SECONDS)).thenReturn(Mono.just(false));

        // when
        Mono<Boolean> result = redissonLockAdapter.acquireLock(rLockReactive);

        // then
        assertFalse(result.block());
        verify(rLockReactive, times(1)).tryLock(3, 3, TimeUnit.SECONDS);
    }

    @Test
    void 락을_획득한_경우_락을_해제한다() {
        // given
        when(redissonReactiveClient.getLock("lockKey")).thenReturn(rLockReactive);
        when(rLockReactive.tryLock(3, 3, TimeUnit.SECONDS)).thenReturn(Mono.just(true));
        when(rLockReactive.unlock()).thenReturn(Mono.empty());

        // when
        redissonLockAdapter.acquireLock(rLockReactive).block();
        Mono<Boolean> result = redissonLockAdapter.releaseLock(rLockReactive, true);

        // then
        assertTrue(result.block());
        verify(rLockReactive, times(1)).unlock();
    }

    @Test
    void 락을_획득하지_못한_경우_락을_해제하지_않는다() {
        // given
        when(redissonReactiveClient.getLock("lockKey")).thenReturn(rLockReactive);
        when(rLockReactive.tryLock(3, 3, TimeUnit.SECONDS)).thenReturn(Mono.just(false));

        // when
        redissonLockAdapter.acquireLock(rLockReactive).block();
        Mono<Boolean> result = redissonLockAdapter.releaseLock(rLockReactive, false);

        // then
        assertFalse(result.block());
        verify(rLockReactive, never()).unlock();
    }


    @Test
    void 락을_획득했는지_확인하는_메서드() {
        // given
        String lockKey = "lockKey";
        when(redissonReactiveClient.getLock(lockKey)).thenReturn(rLockReactive);

        // when
        RLockReactive result = redissonLockAdapter.getLock(lockKey);

        // then
        assertNotNull(result);
        verify(redissonReactiveClient, times(1)).getLock(lockKey);
    }
}
