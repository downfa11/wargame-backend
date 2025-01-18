package com.ns.feed.service;

import com.ns.feed.entity.Image;
import com.ns.feed.repository.ImageR2dbcRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ImageService {
    private final ImageR2dbcRepository imageR2dbcRepository;

    public Mono<Image> createImage(Long postId, String url) {
        Image image = new Image().builder()
                .postId(postId)
                .url(url).build();

        return imageR2dbcRepository.save(image);
    }

    public Mono<Image> updateImage(Long id, Image image) {
        return imageR2dbcRepository.findById(id)
                .flatMap(existingImage -> {
                    existingImage.setPostId(image.getPostId());
                    existingImage.setUrl(image.getUrl());
                    return imageR2dbcRepository.save(existingImage);
                });
    }

    public Mono<List<String>> findImageUrlsById(Long postId) {
        return imageR2dbcRepository.findByPostId(postId)
                .map(Image::getUrl)
                .collectList();
    }

    public Mono<Void> deleteImage(Long id) {
        return imageR2dbcRepository.deleteById(id);
    }

    public Flux<Void> deleteImageByPostId(Long postId){
        return imageR2dbcRepository.findByPostId(postId)
                .flatMap(image -> imageR2dbcRepository.deleteById(image.getId()));
    }
}
