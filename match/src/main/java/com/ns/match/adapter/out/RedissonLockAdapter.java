package com.ns.match.adapter.out;

import com.ns.common.anotation.PersistanceAdapter;
import com.ns.match.application.port.out.lock.AcquireLockPort;
import com.ns.match.application.port.out.lock.GetLockPort;
import com.ns.match.application.port.out.lock.ReleaseLockPort;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLockReactive;
import org.redisson.api.RedissonReactiveClient;
import reactor.core.publisher.Mono;

@PersistanceAdapter
@RequiredArgsConstructor
public class RedissonLockAdapter implements AcquireLockPort, ReleaseLockPort, GetLockPort {
    private final RedissonReactiveClient redissonReactiveClient;


    @Override
    public Mono<Boolean> acquireLock(RLockReactive lock) {
        return lock.tryLock(3, 3, TimeUnit.SECONDS);
    }

    @Override
    public Mono<Boolean> releaseLock(RLockReactive lock, boolean locked) {
        if (locked) {
            return Mono.fromRunnable(() -> lock.unlock())
                    .thenReturn(true);
        }

        return Mono.just(false);
    }


    @Override
    public RLockReactive getLock(String lockKey) {
        return redissonReactiveClient.getLock(lockKey);
    }
}
