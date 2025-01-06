package com.ns.match;

import static org.assertj.core.api.Assertions.assertThat;

import com.ns.common.MessageEntity;
import com.ns.match.service.MatchQueueService;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MatchApplicationTests {

    @Autowired
    private MatchQueueService matchQueueService;

    @Autowired
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    private final String MATCH_WAIT_KEY ="users:queue:%s:wait";


    @Test
    void requestIntegrationTest() {
        AtomicInteger count = new AtomicInteger(0);

        int threads=10, requests=10;

        Flux.range(0, threads)
                .flatMap(thread -> Flux.range(0, requests)
                        .parallel()
                        .runOn(Schedulers.parallel())
                        .flatMap(request -> {
                            System.out.println(thread + " threads - Integration Test [" + request + "] - Matching threads:" + threads + ", requests:" + requests);
                            Long memberId = (long) (Math.random() * 10000);
                            Long elo = 1200 + (long) (Math.random() * 500);
                            String nickName = "test" + (int) (Math.random() * 1000);

                            return requestIntegrationTest(memberId, nickName, elo)
                                    .doOnSuccess(v -> count.incrementAndGet())
                                    .onErrorResume(error -> {
                                        System.err.println("Error requestIntegrationTest: " + error.getMessage());
                                        return Mono.empty();
                                    });
                        })
                        .sequential()
                )
                .then(Mono.fromRunnable(() -> {
                    System.out.println("Test completed: successful requests = " + count.get());
                    assertThat(count.get()).isEqualTo(threads * requests);
                }))
                .block();
    }

    private Mono<Void> requestIntegrationTest(Long memberId, String nickName, Long elo){
        String queue = "integrationTestQueue";
        String memberKey = "user:" + memberId;
        String memberNameField = "nickname";

        return reactiveRedisTemplate.opsForHash()
                .put(memberKey, memberNameField, nickName)
                .then(reactiveRedisTemplate.opsForZSet()
                        .add(MATCH_WAIT_KEY.formatted(queue), memberKey, elo.doubleValue())
                ).then();
    }
}
