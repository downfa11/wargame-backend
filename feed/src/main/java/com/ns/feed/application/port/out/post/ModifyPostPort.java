package com.ns.feed.application.port.out.post;

import com.ns.feed.adapter.out.persistence.post.Post;
import com.ns.feed.dto.PostModifyRequest;
import reactor.core.publisher.Mono;

public interface ModifyPostPort {
    Mono<Post> modifyPost(String nickname, PostModifyRequest request);
    Mono<Post> update(Post post);
}
