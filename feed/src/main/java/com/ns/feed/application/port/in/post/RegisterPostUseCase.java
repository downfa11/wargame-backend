package com.ns.feed.application.port.in.post;

import com.ns.feed.adapter.out.persistence.post.Post;
import com.ns.feed.dto.PostRegisterRequest;
import reactor.core.publisher.Mono;

public interface RegisterPostUseCase {
    Mono<Post> create(Long userId, PostRegisterRequest request);
}
