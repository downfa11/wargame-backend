package com.ns.feed.application.port.in.post;

import com.ns.feed.adapter.out.persistence.post.Post;
import com.ns.feed.dto.PostModifyRequest;
import reactor.core.publisher.Mono;

public interface ModifyPostUseCase {
    Mono<Post> modify(Long userId, PostModifyRequest request);
}
