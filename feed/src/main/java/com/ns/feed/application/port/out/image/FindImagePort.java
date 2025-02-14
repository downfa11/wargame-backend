package com.ns.feed.application.port.out.image;

import java.util.List;
import reactor.core.publisher.Mono;

public interface FindImagePort {
    Mono<List<String>> findImageUrlsById(Long postId);
}
