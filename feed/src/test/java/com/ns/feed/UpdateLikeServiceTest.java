package com.ns.feed;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ns.feed.application.port.out.like.AddLikePort;
import com.ns.feed.application.port.out.like.FindLikePort;
import com.ns.feed.application.port.out.like.RemoveLikePort;
import com.ns.feed.application.service.UpdateLikeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
public class UpdateLikeServiceTest {

    @Mock private AddLikePort addLikePort;
    @Mock private FindLikePort findLikePort;
    @Mock private RemoveLikePort removeLikePort;

    @InjectMocks
    private UpdateLikeService updateLikeService;

    private final Long userId = 1L;
    private final Long boardId = 10L;

    @Test
    public void 아직_좋아요를_누르지_않은_사용자의_updateLikes(){
        // given
        when(findLikePort.isUserLiked(boardId, userId)).thenReturn(Mono.just(false));
        when(findLikePort.getLikesCount(boardId)).thenReturn(Mono.just(1L));
        when(addLikePort.addLike(boardId, String.valueOf(userId))).thenReturn(Mono.empty());
        // when
        when(findLikePort.getLikesCount(boardId)).thenReturn(Mono.just(2L));
        Mono<Long> result = updateLikeService.updateLikes(userId, boardId);
        // then
        StepVerifier.create(result)
                .expectNext(2L)
                .verifyComplete();

        verify(addLikePort).addLike(boardId, String.valueOf(userId));
        verify(findLikePort).getLikesCount(boardId);
    }

    @Test
    public void 이미_좋아요를_누른_사용자의_updateLikes(){
        // given
        when(findLikePort.isUserLiked(boardId, userId)).thenReturn(Mono.just(true));
        when(findLikePort.getLikesCount(boardId)).thenReturn(Mono.just(2L));
        when(removeLikePort.removeLike(boardId, userId)).thenReturn(Mono.empty());
        // when
        when(findLikePort.getLikesCount(boardId)).thenReturn(Mono.just(1L));
        Mono<Long> result = updateLikeService.updateLikes(userId, boardId);
        // then
        StepVerifier.create(result)
                .expectNext(1L)
                .verifyComplete();

        verify(removeLikePort).removeLike(boardId, userId);
        verify(findLikePort).getLikesCount(boardId);
    }
}
