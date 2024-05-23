package com.ns.wargame.Repository;

import com.ns.wargame.Domain.Post;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;

public interface PostCustomR2dbcRepository {
    Flux<Post> findAllByUserId(Long userId);
}
