package com.ns.feed.adapter.out.persistence.image;

import com.ns.common.anotation.PersistanceAdapter;
import com.ns.feed.application.port.out.image.DeleteImagePort;
import com.ns.feed.application.port.out.image.FindImagePort;
import com.ns.feed.application.port.out.image.UpdateImagePort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@PersistanceAdapter
@RequiredArgsConstructor
public class ImagePersistenceAdapter implements UpdateImagePort, DeleteImagePort, FindImagePort {

    private final ImageR2dbcRepository imageR2dbcRepository;

    @Override
    public Mono<Image> createImage(Long postId, String url) {
        Image image = new Image().builder()
                .postId(postId)
                .url(url).build();

        return imageR2dbcRepository.save(image);
    }

    @Override
    public Mono<Image> updateImage(Long id, Image image) {
        return imageR2dbcRepository.findById(id)
                .flatMap(existingImage -> {
                    existingImage.setPostId(image.getPostId());
                    existingImage.setUrl(image.getUrl());
                    return imageR2dbcRepository.save(existingImage);
                });
    }

    @Override
    public Mono<Void> deleteImage(Long id) {
        return imageR2dbcRepository.deleteById(id);
    }

    @Override
    public Flux<Void> deleteImageByPostId(Long postId) {
        return imageR2dbcRepository.findByPostId(postId)
                .flatMap(image -> imageR2dbcRepository.deleteById(image.getId()));
    }

    @Override
    public Mono<List<String>> findImageUrlsById(Long postId) {
        return imageR2dbcRepository.findByPostId(postId)
                .map(Image::getUrl)
                .collectList();
    }
}
