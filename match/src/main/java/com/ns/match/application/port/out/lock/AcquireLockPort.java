package com.ns.match.application.port.out.lock;

import org.redisson.api.RLockReactive;
import reactor.core.publisher.Mono;

public interface AcquireLockPort {
    Mono<Boolean> acquireLock(RLockReactive lock);
}
