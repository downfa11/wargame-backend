package com.ns.feed.application.port.out.image;

import com.ns.feed.adapter.out.persistence.image.Image;
import reactor.core.publisher.Mono;

public interface UpdateImagePort {
    Mono<Image> createImage(Long postId, String url);
    Mono<Image> updateImage(Long id, Image image);
}
