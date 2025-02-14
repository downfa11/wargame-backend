package com.ns.match.application.service;

import com.ns.common.anotation.UseCase;
import com.ns.common.utils.MessageEntity;
import com.ns.match.application.port.in.IntegrationTestMatchUseCase;
import com.ns.match.application.port.out.IntegrationTestMatchPort;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class IntegrationTestService implements IntegrationTestMatchUseCase {
    private final IntegrationTestMatchPort integrationTestMatchPort;

    @Override
    public Mono<Long> requestIntegrationTest(int threads, int requests) {
        AtomicLong memberIdGenerator = new AtomicLong(1);

        return Flux.range(0, threads)
                .flatMap(thread -> Flux.range(0, requests)
                        .flatMap(request -> {
                            Long memberId = memberIdGenerator.getAndIncrement();
                            Long elo = 1200 + (long) (Math.random() * 500);
                            String nickName = "test" + memberId;

                            return integrationTestMatchPort.requestIntegrationTest(memberId, nickName, elo)
                                    .doOnError(error -> log.error("Error requestIntegrationTest: " + error.getMessage()))
                                    .retry(3)
                                    .onErrorResume(error -> Mono.empty());
                        }, 1)
                )
                .then(integrationTestMatchPort.getRequestCount());
    }
}
