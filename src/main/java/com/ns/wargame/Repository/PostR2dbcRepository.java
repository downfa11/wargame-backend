package com.ns.wargame.Repository;

import com.ns.wargame.Domain.Post;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface PostR2dbcRepository extends ReactiveCrudRepository<Post,Long>, PostCustomR2dbcRepository {

    Flux<Post> findByuserId(Long id);
}
