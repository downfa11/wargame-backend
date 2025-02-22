package com.ns.match.application.port.out.lock;

import org.redisson.api.RLockReactive;

public interface GetLockPort {
    RLockReactive getLock(String lockKey);
}
