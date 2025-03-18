package com.ns.feed.adapter;


import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ns.feed.adapter.out.persistence.LikePersistenceAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveSetOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class LikePersistenceAdapterTest {

    @Mock private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    @Mock private ReactiveSetOperations<String, String> setOperations;

    @InjectMocks private LikePersistenceAdapter likePersistenceAdapter;

    private static final Long BOARD_ID = 1L;
    private static final Long USER_ID = 123L;
    private static final String BOARD_LIKES_KEY = "boards:likes:1";



    @Test
    void 게시글에_좋아요를_추가하는_메서드() {
        when(reactiveRedisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.add(BOARD_LIKES_KEY, String.valueOf(USER_ID))).thenReturn(Mono.just(1L));

        StepVerifier.create(likePersistenceAdapter.addLike(BOARD_ID, String.valueOf(USER_ID)))
                .expectNext(1L)
                .verifyComplete();

        verify(setOperations, times(1)).add(BOARD_LIKES_KEY, String.valueOf(USER_ID));
    }

    @Test
    void 게시글의_좋아요_수를_조회하는_메서드() {
        when(reactiveRedisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.size(BOARD_LIKES_KEY)).thenReturn(Mono.just(10L));

        StepVerifier.create(likePersistenceAdapter.getLikesCount(BOARD_ID))
                .expectNext(10L)
                .verifyComplete();

        verify(setOperations, times(1)).size(BOARD_LIKES_KEY);
    }

    @Test
    void 사용자가_좋아요를_눌렀는지_확인하는_메서드() {
        when(reactiveRedisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.isMember(BOARD_LIKES_KEY, USER_ID)).thenReturn(Mono.just(true));

        StepVerifier.create(likePersistenceAdapter.isUserLiked(BOARD_ID, Long.valueOf(USER_ID)))
                .expectNext(true)
                .verifyComplete();

        verify(setOperations, times(1)).isMember(BOARD_LIKES_KEY, USER_ID);
    }

    @Test
    void 게시글에서_좋아요를_삭제하는_메서드() {
        when(reactiveRedisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.remove(BOARD_LIKES_KEY, USER_ID)).thenReturn(Mono.just(1L));

        StepVerifier.create(likePersistenceAdapter.removeLike(BOARD_ID, Long.valueOf(USER_ID)))
                .expectNext(1L)
                .verifyComplete();

        verify(setOperations, times(1)).remove(BOARD_LIKES_KEY, USER_ID);
    }

    @Test
    void 게시글의_모든_좋아요를_삭제하는_메서드() {
        when(reactiveRedisTemplate.unlink(BOARD_LIKES_KEY)).thenReturn(Mono.just(1L));

        StepVerifier.create(likePersistenceAdapter.removeLikeAllByPostId(BOARD_ID))
                .expectNext(1L)
                .verifyComplete();

        verify(reactiveRedisTemplate, times(1)).unlink(BOARD_LIKES_KEY);
    }
}