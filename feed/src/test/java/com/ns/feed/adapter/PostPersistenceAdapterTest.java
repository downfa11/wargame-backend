package com.ns.feed.adapter;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ns.feed.adapter.out.persistence.post.Post;
import com.ns.feed.adapter.out.persistence.post.Post.SortStatus;
import com.ns.feed.adapter.out.persistence.post.PostPersistenceAdapter;
import com.ns.feed.adapter.out.persistence.post.PostR2dbcRepository;
import com.ns.feed.dto.PostModifyRequest;
import com.ns.feed.exception.FeedException;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class PostPersistenceAdapterTest {

    @Mock
    private PostR2dbcRepository postR2dbcRepository;

    private PostPersistenceAdapter postPersistenceAdapter;

    private Post post;
    private PostModifyRequest request;

    @BeforeEach
    void init() {
        postPersistenceAdapter = new PostPersistenceAdapter(postR2dbcRepository);
        post = Post.builder()
                .id(1L)
                .userId(1L)
                .title("title")
                .nickname("user")
                .content("contents")
                .categoryId(1L)
                .build();

        request = PostModifyRequest.builder()
                .boardId(1L)
                .categoryId(1L)
                .sortStatus(SortStatus.EVENT)
                .title("Updated Title")
                .content("Updated Content")
                .eventStartDate(null)
                .eventEndDate(null)
                .build();
    }

    @Test
    void 게시글을_등록하는_메서드() {
        // given
        when(postR2dbcRepository.save(any(Post.class))).thenReturn(Mono.just(post));

        // when
        Post result = postPersistenceAdapter.registerPost(post).block();

        // then
        assertNotNull(result);
        assertEquals("title", result.getTitle());
        verify(postR2dbcRepository, times(1)).save(post);
    }

    @Test
    void 게시글을_수정하는_메서드() {
        // given
        Long postId = 1L;
        when(postR2dbcRepository.findById(postId)).thenReturn(Mono.just(post));
        when(postR2dbcRepository.save(any(Post.class))).thenReturn(Mono.just(post));

        // when
        Post result = postPersistenceAdapter.modifyPost("user", request).block();

        // then
        assertNotNull(result);
        assertEquals("Updated Title", result.getTitle());
        assertEquals("Updated Content", result.getContent());
        verify(postR2dbcRepository, times(1)).save(post);
    }

    @Test
    void 게시글을_수정하는_메서드_존재하지_않는_경우() {
        // given
        Long postId = request.getBoardId();
        when(postR2dbcRepository.findById(postId)).thenReturn(Mono.empty());

        // when & then
        assertThrows(FeedException.class, () -> postPersistenceAdapter.modifyPost("user", request).block());
    }

    @Test
    void 게시글을_삭제하는_메서드() {
        // given
        Long postId = 5L;

        when(postR2dbcRepository.deleteById(postId)).thenReturn(Mono.empty());

        // when
        postPersistenceAdapter.deletePost(postId).block();

        // then
        verify(postR2dbcRepository, times(1)).deleteById(postId);
    }

    @Test
    void postId로_게시글을_조회하는_메서드() {
        // given
        Long postId = 1L;

        when(postR2dbcRepository.findById(postId)).thenReturn(Mono.just(post));

        // when
        Post result = postPersistenceAdapter.findPostByPostId(postId).block();

        // then
        assertNotNull(result);
        assertEquals("title", result.getTitle());
        verify(postR2dbcRepository, times(1)).findById(postId);
    }

    @Test
    void postId로_게시글을_조회하는_메서드_존재하지_않는_경우() {
        // given
        Long postId = 99L;
        when(postR2dbcRepository.findById(postId)).thenReturn(Mono.empty());

        // when
        Post result = postPersistenceAdapter.findPostByPostId(postId).block();

        // then
        assertNull(result);
    }

    @Test
    void 카테고리별_게시글_개수를_조회하는_메서드() {
        // given
        Long categoryId = 1L;
        when(postR2dbcRepository.countByCategoryId(categoryId)).thenReturn(Mono.just(10L));

        // when
        Long result = postPersistenceAdapter.countByCategoryId(categoryId).block();

        // then
        assertEquals(10L, result);
    }

    @Test
    void membershipId로_게시글을_조회하는_메서드() {
        // given
        Long membershipId = 100L;
        when(postR2dbcRepository.findAllByUserId(membershipId)).thenReturn(Flux.just(post));

        // when
        List<Post> result = postPersistenceAdapter.findPostsByMembershipId(membershipId).collectList().block();

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void 최신_공지사항을_조회하는_메서드() {
        // given
        int count = 5;
        when(postR2dbcRepository.findLatestAnnounces(count)).thenReturn(Flux.empty());

        // when
        List<Post> result = postPersistenceAdapter.findLatestAnnounces(count).collectList().block();

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void 진행중인_이벤트를_조회하는_메서드() {
        // given
        LocalDateTime now = LocalDateTime.now();
        when(postR2dbcRepository.findInProgressEvents(now)).thenReturn(Flux.empty());

        // when
        List<Post> result = postPersistenceAdapter.findInProgressEvents(now).collectList().block();

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
