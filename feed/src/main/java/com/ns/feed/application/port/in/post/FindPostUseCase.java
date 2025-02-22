package com.ns.feed.application.port.in.post;

import com.ns.feed.adapter.out.persistence.post.Post;
import com.ns.feed.dto.BannerListWrapper;
import com.ns.feed.dto.PostResponse;
import com.ns.feed.dto.PostSummary;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FindPostUseCase {
    Mono<Void> postByMembershipId(String taskId, String membershipId);
    Flux<Post> findPostsAll();
    Mono<Page<PostSummary>> findPostAllPagination(Long categoryId, PageRequest pageRequest);
    Mono<PostResponse> findPostResponseById(Long membershipId);
    Mono<PostResponse> updatePostResponseById(Long membershipId);
    Mono<BannerListWrapper> findLatestAnnounces(Integer count);
    Mono<BannerListWrapper> findInProgressEvents();
}
