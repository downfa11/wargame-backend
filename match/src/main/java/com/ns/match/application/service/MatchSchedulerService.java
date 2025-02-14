package com.ns.match.application.service;

import com.ns.match.application.port.out.ProcessMatchQueuePort;
import com.ns.match.application.port.out.lock.AcquireLockPort;
import com.ns.match.application.port.out.lock.GetLockPort;
import com.ns.match.application.port.out.lock.ReleaseLockPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLockReactive;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchSchedulerService {

    @Value("${scheduler.enabled}")
    private Boolean scheduling = false;

    private final String MATCH_WAIT_KEY_FOR_SCAN ="users:queue:*:wait";

    private final ProcessMatchQueuePort processMatchQueuePort;

    private final AcquireLockPort acquireLockPort;
    private final GetLockPort getLockPort;
    private final ReleaseLockPort releaseLockPort;


    @Scheduled(initialDelay = 3000, fixedDelay = 3000)
    public Mono<Void> scheduleMatchUser() {
        if (!scheduling) {
            log.info("passed scheduling..");
            return Mono.empty();
        }

        String lockKey = "lock:scheduleMatchUser";
        RLockReactive lock = getLockPort.getLock(lockKey);

        return acquireLockPort.acquireLock(lock)
                .flatMap(locked -> {
                    if (!locked) {
                        return Mono.empty();
                    }
                    return processMatchQueuePort.process(MATCH_WAIT_KEY_FOR_SCAN)
                            .thenReturn(locked);
                })
                .flatMap(locked -> releaseLockPort.releaseLock(lock, locked))
                .then();
    }
}
