package com.ns.feed.repository;

import com.ns.feed.entity.Post;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;

public interface PostCustomR2dbcRepository {
    Flux<Post> findAllByUserId(Long userId);
}
