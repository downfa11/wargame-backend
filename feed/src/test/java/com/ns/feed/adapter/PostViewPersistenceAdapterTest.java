package com.ns.feed.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ns.feed.adapter.out.persistence.PostViewPersistenceAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
public class PostViewPersistenceAdapterTest {

    @Mock private ReactiveRedisTemplate<String, Long> reactiveRedisTemplate;
    @Mock private ReactiveValueOperations<String, Long> valueOperations;

    private PostViewPersistenceAdapter postViewPersistenceAdapter;

    @BeforeEach
    void init() {
        postViewPersistenceAdapter = new PostViewPersistenceAdapter(reactiveRedisTemplate);
    }

    @Test
    void 게시글_조회수를_조회하는_메서드_초기값은_0() {
        // given
        Long boardId = 1L;
        String key = "boards:views:" + boardId;

        when(reactiveRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn(Mono.empty());

        // when
        Long views = postViewPersistenceAdapter.getPostViews(boardId).block();

        // then
        assertEquals(0L, views);
        verify(valueOperations, times(1)).get(key);
    }

    @Test
    void 게시글_조회수를_조회하는_메서드() {
        // given
        Long boardId = 1L;
        String key = "boards:views:" + boardId;

        when(reactiveRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn(Mono.just(5L));

        // when
        Long views = postViewPersistenceAdapter.getPostViews(boardId).block();

        // then
        assertEquals(5L, views);
        verify(valueOperations, times(1)).get(key);
    }

    @Test
    void 게시글_조회수를_증가시키는_메서드() {
        // given
        Long boardId = 1L;
        String key = "boards:views:" + boardId;

        when(reactiveRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(key)).thenReturn(Mono.just(6L));

        // when
        Long newViews = postViewPersistenceAdapter.incrPostViews(boardId).block();

        // then
        assertEquals(6L, newViews);
        verify(valueOperations, times(1)).increment(key);
    }

    @Test
    void 게시글_조회수를_삭제하는_메서드() {
        // given
        Long boardId = 1L;
        String key = "boards:views:" + boardId;

        when(reactiveRedisTemplate.unlink(key)).thenReturn(Mono.just(1L));

        // when
        Long result = postViewPersistenceAdapter.removePostView(boardId).block();

        // then
        assertEquals(1L, result);
        verify(reactiveRedisTemplate, times(1)).unlink(key);
    }
}