package com.ns.feed.adapter.out.persistence;


import com.ns.common.anotation.PersistanceAdapter;
import com.ns.feed.application.port.out.like.AddLikePort;
import com.ns.feed.application.port.out.like.FindLikePort;
import com.ns.feed.application.port.out.like.RemoveLikePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Mono;


@PersistanceAdapter
@Slf4j
@RequiredArgsConstructor
public class LikePersistenceAdapter implements AddLikePort, RemoveLikePort, FindLikePort {

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final String BOARD_LIKES_KEY = "boards:likes:%s";

    @Override
    public Mono<Long> addLike(Long boardId, String userId) {
        String key = String.format(BOARD_LIKES_KEY, boardId);
        return reactiveRedisTemplate.opsForSet().add(key, userId);
    }

    @Override
    public Mono<Long> getLikesCount(Long boardId) {
        String key = String.format(BOARD_LIKES_KEY, boardId);
        return reactiveRedisTemplate.opsForSet().size(key);
    }

    // 해당 사용자는 좋아요를 눌렀는가?
    @Override
    public Mono<Boolean> isUserLiked(Long boardId, Long userId) {
        String key = String.format(BOARD_LIKES_KEY, boardId);
        return reactiveRedisTemplate.opsForSet().isMember(key, userId);
    }

    @Override
    public Mono<Long> removeLike(Long boardId, Long userId) {
        String key = String.format(BOARD_LIKES_KEY, boardId);
        return reactiveRedisTemplate.opsForSet().remove(key, userId);
    }

    @Override
    public Mono<Long> removeLikeAllByPostId(Long boardId) {
        return reactiveRedisTemplate.unlink(BOARD_LIKES_KEY.formatted(boardId));
    }
}
