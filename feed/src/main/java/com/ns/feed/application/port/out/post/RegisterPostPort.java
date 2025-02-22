package com.ns.feed.application.port.out.post;

import com.ns.feed.adapter.out.persistence.post.Post;
import com.ns.feed.dto.PostRegisterRequest;
import reactor.core.publisher.Mono;

public interface RegisterPostPort {
    Mono<Post> registerPost(Post post);
}
