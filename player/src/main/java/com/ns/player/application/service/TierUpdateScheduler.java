package com.ns.player.application.service;

import com.ns.player.adapter.out.persistence.Player;
import com.ns.player.adapter.out.persistence.Tier;
import com.ns.player.application.port.out.FindPlayerPort;
import com.ns.player.application.port.out.UpdatePlayerPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class TierUpdateScheduler {

    private final RedissonClient redissonClient;
    private final FindPlayerPort findPlayerPort;
    private final UpdatePlayerPort updatePlayerPort;

    private static final String LOCK_NAME = "tier-update-lock";

    @Scheduled(cron = "0 0 22 ? * SUN", zone = "Asia/Seoul")
    public void updateRankerTier() throws InterruptedException {
        RLock lock = redissonClient.getLock(LOCK_NAME);

        if (lock.tryLock(10, 300, TimeUnit.SECONDS)) {
            try {
                doRankerUpdate()
                        .doOnError(e -> log.error("랭커 갱신 실패", e))
                        .subscribe();
            } finally {
                lock.unlock();
            }
        } else {
            log.info("이미 다른 인스턴스가 갱신 작업을 수행 중입니다.");
        }
    }

    private Mono<Void> doRankerUpdate() {
        return findPlayerPort.findTopRankedPlayers(10)
                .collectList()
                .flatMapMany(top10Players -> {
                    Set<String> top10MembershipIds = top10Players.stream()
                            .map(Player::getMembershipId)
                            .collect(Collectors.toSet());

                    return findPlayerPort.findAll()
                            .flatMap(player -> {
                                String membershipId = player.getMembershipId();
                                Tier currentTier = player.getTier();

                                // 상위 10명을 랭커로 설정
                                if (top10MembershipIds.contains(membershipId)) {
                                    if (currentTier != Tier.RANKER) {
                                        return updatePlayerPort.updateRankerTier(membershipId, Tier.RANKER);
                                    } else {
                                        return Mono.empty();
                                    }
                                }

                                // 기존 랭커중에서 탈락자 강등
                                if (currentTier == Tier.RANKER) {
                                    Tier newTier = Tier.fromElo(player.getElo());
                                    return updatePlayerPort.updateRankerTier(membershipId, newTier);
                                }
                                return Mono.empty();
                            });
                })
                .then();
    }


}
