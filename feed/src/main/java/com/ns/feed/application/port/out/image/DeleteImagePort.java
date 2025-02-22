package com.ns.feed.application.port.out.image;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DeleteImagePort {
    Mono<Void> deleteImage(Long id);
    Flux<Void> deleteImageByPostId(Long postId);
}
