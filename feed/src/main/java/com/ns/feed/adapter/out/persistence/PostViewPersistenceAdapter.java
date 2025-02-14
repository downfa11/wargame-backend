package com.ns.feed.adapter.out.persistence;


import com.ns.common.anotation.PersistanceAdapter;
import com.ns.feed.application.port.out.like.FindPostViewPort;
import com.ns.feed.application.port.out.like.IncreasePostViewPort;
import com.ns.feed.application.port.out.like.RemovePostViewPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Mono;

@PersistanceAdapter
@Slf4j
@RequiredArgsConstructor
public class PostViewPersistenceAdapter implements IncreasePostViewPort, RemovePostViewPort, FindPostViewPort {

    private final ReactiveRedisTemplate<String, Long> reactiveRedisTemplate;
    private final String BOARD_VIEWS_KEY = "boards:views:%s";

    @Override
    public Mono<Long> getPostViews(Long boardId) {
        return reactiveRedisTemplate.opsForValue()
                .get(BOARD_VIEWS_KEY.formatted(boardId))
                .switchIfEmpty(Mono.just(0L));
    }

    @Override
    public Mono<Long> incrPostViews(Long boardId) {
        return reactiveRedisTemplate.opsForValue()
                .increment(BOARD_VIEWS_KEY.formatted(boardId))
                .switchIfEmpty(Mono.just(0L));
    }

    @Override
    public Mono<Long> removePostView(Long boardId) {
        return reactiveRedisTemplate.unlink(BOARD_VIEWS_KEY.formatted(boardId));
    }
}
