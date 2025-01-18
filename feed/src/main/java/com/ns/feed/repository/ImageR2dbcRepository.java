package com.ns.feed.repository;


import com.ns.feed.entity.Image;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface ImageR2dbcRepository extends ReactiveCrudRepository<Image,Long> {
    Flux<Image> findByPostId(Long postId);
}