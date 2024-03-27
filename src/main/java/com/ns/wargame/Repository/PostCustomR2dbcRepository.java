package com.ns.wargame.Repository;

import com.ns.wargame.Domain.Post;
import reactor.core.publisher.Flux;

public interface PostCustomR2dbcRepository {
    Flux<Post> findAllByUserId(Long userId);

}
