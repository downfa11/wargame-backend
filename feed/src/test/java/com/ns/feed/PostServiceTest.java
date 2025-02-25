package com.ns.feed;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.ns.feed.adapter.out.persistence.post.Post;
import com.ns.feed.application.port.out.TaskProducerPort;
import com.ns.feed.application.port.out.comment.DeleteCommentPort;
import com.ns.feed.application.port.out.comment.FindCommentPort;
import com.ns.feed.application.port.out.like.FindLikePort;
import com.ns.feed.application.port.out.like.FindPostViewPort;
import com.ns.feed.application.port.out.like.IncreasePostViewPort;
import com.ns.feed.application.port.out.like.RemoveLikePort;
import com.ns.feed.application.port.out.like.RemovePostViewPort;
import com.ns.feed.application.port.out.post.DeletePostPort;
import com.ns.feed.application.port.out.post.FindPostPort;
import com.ns.feed.application.port.out.post.ModifyPostPort;
import com.ns.feed.application.port.out.post.RegisterPostPort;
import com.ns.feed.application.service.PostService;
import com.ns.feed.dto.PostModifyRequest;
import com.ns.feed.dto.PostRegisterRequest;
import com.ns.feed.dto.PostResponse;
import com.ns.feed.exception.FeedException;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
public class PostServiceTest {
    @Mock private RegisterPostPort registerPostPort;
    @Mock private ModifyPostPort modifyPostPort;
    @Mock private DeletePostPort deletePostPort;
    @Mock private FindPostPort findPostPort;

    @Mock private IncreasePostViewPort increasePostViewPort;
    @Mock private RemovePostViewPort removePostViewPort;
    @Mock private FindPostViewPort findPostViewPort;
    @Mock private RemoveLikePort removeLikePort;
    @Mock private FindLikePort findLikePort;
    @Mock private FindCommentPort findCommentPort;
    @Mock private DeleteCommentPort deleteCommentPort;
    @Mock private TaskProducerPort taskProducerPort;


    @InjectMocks private PostService postService;


    @Test
    void 게시글을_작성하는_테스트() {
        // Given
        when(taskProducerPort.getUserNameByPost(1L)).thenReturn(Mono.just("User"));
        when(registerPostPort.registerPost(any())).thenReturn(Mono.just(Post.builder().build()));
        // When
        PostRegisterRequest request = PostRegisterRequest.builder().build();
        Mono<Post> result = postService.create(1L, request);

        // Then
        StepVerifier.create(result)
                .expectNextCount(1)
                .verifyComplete();

        verify(taskProducerPort, times(1)).getUserNameByPost(1L);
        verify(registerPostPort, times(1)).registerPost(any());
    }

    @Test
    void 게시글을_수정하는_테스트() {
        // Given
        when(taskProducerPort.getUserNameByPost(1L)).thenReturn(Mono.just("User"));
        when(modifyPostPort.modifyPost(any(), any())).thenReturn(Mono.just(Post.builder().build()));

        // When
        PostModifyRequest request = PostModifyRequest.builder().build();
        Mono<Post> result = postService.modify(1L, request);

        // Then
        StepVerifier.create(result)
                .expectNextCount(1)
                .verifyComplete();

        verify(taskProducerPort, times(1)).getUserNameByPost(1L);
        verify(modifyPostPort, times(1)).modifyPost(any(), any());
    }

    @Test
    void 게시글을_삭제하는_메서드() {
        // Given
        Long postId = 1L;
        when(findPostPort.findPostByPostId(postId)).thenReturn(Mono.just(Post.builder().build()));
        when(removeLikePort.removeLikeAllByPostId(postId)).thenReturn(Mono.empty());
        when(removePostViewPort.removePostView(postId)).thenReturn(Mono.empty());
        when(deleteCommentPort.deleteByBoardId(postId)).thenReturn(Flux.empty());
        when(deletePostPort.deletePost(postId)).thenReturn(Mono.empty());

        // When
        Mono<Void> result = postService.deleteById(postId);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(findPostPort, times(1)).findPostByPostId(postId);
        verify(removeLikePort, times(1)).removeLikeAllByPostId(postId);
        verify(removePostViewPort, times(1)).removePostView(postId);
        verify(deleteCommentPort, times(1)).deleteByBoardId(postId);
        verify(deletePostPort, times(1)).deletePost(postId);
    }

    @Test
    void 게시글을_삭제하고_싶은데_없는_경우() {
        // Given
        Long postId = 1L;
        when(findPostPort.findPostByPostId(postId)).thenReturn(Mono.empty());

        // When
        Mono<Void> result = postService.deleteById(postId);

        // Then
        StepVerifier.create(result)
                .expectError(FeedException.class)
                .verify();

        verify(findPostPort, times(1)).findPostByPostId(postId);
        verifyNoMoreInteractions(removeLikePort, removePostViewPort, deleteCommentPort, deletePostPort);
    }

    @Test
    void PostId로_PostResponse를_조회하는_메서드() {
        // Given
        Long postId = 1L;
        Post post = Post.builder().build();
        post.setId(postId);
        when(findPostPort.findPostByPostId(postId)).thenReturn(Mono.just(post));
        when(findLikePort.getLikesCount(postId)).thenReturn(Mono.just(10L));
        when(increasePostViewPort.incrPostViews(anyLong())).thenReturn(Mono.just(101L));
        when(findCommentPort.findCommentResponseByPostId(postId)).thenReturn(Flux.empty());

        // When
        Mono<PostResponse> result = postService.findPostResponseById(postId);

        // Then
        PostResponse expectedResponse = PostResponse.builder()
                .id(postId)
                .userId(null)
                .nickname(null)
                .categoryId(null)
                .sortStatus(null)
                .title(null)
                .content(null)
                .imageUrls(null)
                .commentList(Collections.emptyList())
                .comments(null)
                .likes(10l)
                .views(101l)
                .createdAt(null)
                .updatedAt(null)
                .build();

        StepVerifier.create(result)
                .expectNext(expectedResponse)
                .verifyComplete();

        verify(findPostPort, times(1)).findPostByPostId(postId);
        verify(findLikePort, times(1)).getLikesCount(postId);
    }
}
