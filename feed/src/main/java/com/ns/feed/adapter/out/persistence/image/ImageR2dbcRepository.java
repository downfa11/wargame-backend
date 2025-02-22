package com.ns.feed.adapter.out.persistence.image;


import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface ImageR2dbcRepository extends ReactiveCrudRepository<Image,Long> {
    Flux<Image> findByPostId(Long postId);
}