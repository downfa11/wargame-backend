package com.ns.feed.application.port.out.post;

import com.ns.feed.adapter.out.persistence.post.Post;
import com.ns.feed.dto.BannerListWrapper;
import com.ns.feed.dto.PostResponse;
import com.ns.feed.dto.PostSummary;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FindPostPort {
    Mono<Post> findPostByPostId(Long postId);
    Mono<Long> countByCategoryId(Long categoryId);
    Flux<Post> findPostsByMembershipId(Long membershipId);
    Flux<Post> findAllByCategoryId(Long categoryId, PageRequest pageRequest);
    Flux<Post> findPostsAll();
    Flux<Post> findLatestAnnounces(Integer count);
    Flux<Post> findInProgressEvents(LocalDateTime now);
}
