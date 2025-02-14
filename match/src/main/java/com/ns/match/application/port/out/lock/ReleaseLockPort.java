package com.ns.match.application.port.out.lock;

import org.redisson.api.RLockReactive;
import reactor.core.publisher.Mono;

public interface ReleaseLockPort {
    Mono<Boolean> releaseLock(RLockReactive lock, boolean locked);
}
