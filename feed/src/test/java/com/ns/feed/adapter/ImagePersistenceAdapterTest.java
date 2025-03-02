package com.ns.feed.adapter;


import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.ns.feed.adapter.out.persistence.image.Image;
import com.ns.feed.adapter.out.persistence.image.ImagePersistenceAdapter;
import com.ns.feed.adapter.out.persistence.image.ImageR2dbcRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class ImagePersistenceAdapterTest {

    @Mock private ImageR2dbcRepository imageR2dbcRepository;

    private ImagePersistenceAdapter imagePersistenceAdapter;
    private Image image;

    @BeforeEach
    void init() {
        imagePersistenceAdapter = new ImagePersistenceAdapter(imageR2dbcRepository);

        image = Image.builder()
                .id(1L)
                .postId(10L)
                .url("http://image.com/test.jpg")
                .build();
    }

    @Test
    void 이미지를_생성하는_메서드() {
        // given
        Long postId = image.getPostId();
        String url = image.getUrl();

        when(imageR2dbcRepository.save(any(Image.class))).thenReturn(Mono.just(image));

        // when
        Image result = imagePersistenceAdapter.createImage(postId, url).block();

        // then
        assertNotNull(result);
        assertEquals(postId, result.getPostId());
        assertEquals(url, result.getUrl());
        verify(imageR2dbcRepository, times(1)).save(any(Image.class));
    }

    @Test
    void 이미지를_수정하는_메서드() {
        // given
        Long imageId = image.getId();
        Long newPostId = 20L;
        String newUrl = "http://image.com/updated.jpg";

        Image newImageData = Image.builder()
                .id(imageId)
                .postId(newPostId)
                .url(newUrl)
                .build();

        when(imageR2dbcRepository.findById(imageId)).thenReturn(Mono.just(image));
        when(imageR2dbcRepository.save(any(Image.class))).thenReturn(Mono.just(image));

        // when
        Image result = imagePersistenceAdapter.updateImage(imageId, newImageData).block();

        // then
        assertNotNull(result);
        assertEquals(newPostId, result.getPostId());
        assertEquals(newUrl, result.getUrl());
        verify(imageR2dbcRepository, times(1)).save(image);
    }

    @Test
    void 이미지를_수정하는_메서드_존재하지_않는_경우() {
        // given
        Long imageId = 99L;
        when(imageR2dbcRepository.findById(imageId)).thenReturn(Mono.empty());

        // when
        Image result = imagePersistenceAdapter.updateImage(imageId, image).block();

        // then
        assertNull(result);
        verify(imageR2dbcRepository, never()).save(any(Image.class));
    }

    @Test
    void 이미지를_삭제하는_메서드() {
        // given
        Long imageId = 5L;
        when(imageR2dbcRepository.deleteById(imageId)).thenReturn(Mono.empty());

        // when
        imagePersistenceAdapter.deleteImage(imageId).block();

        // then
        verify(imageR2dbcRepository, times(1)).deleteById(imageId);
    }

    @Test
    void postId로_이미지를_삭제하는_메서드() {
        // given
        Long postId = image.getPostId();
        when(imageR2dbcRepository.findByPostId(postId)).thenReturn(Flux.just(image));
        when(imageR2dbcRepository.deleteById(anyLong())).thenReturn(Mono.empty());

        // when
        imagePersistenceAdapter.deleteImageByPostId(postId).blockLast();

        // then
        verify(imageR2dbcRepository, times(1)).deleteById(anyLong());
    }

    @Test
    void postId로_이미지_URL을_조회하는_메서드() {
        // given
        Long postId = image.getPostId();
        String url1 = image.getUrl();

        when(imageR2dbcRepository.findByPostId(postId)).thenReturn(Flux.just(image));

        // when
        List<String> result = imagePersistenceAdapter.findImageUrlsById(postId).block();

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(url1));
        verify(imageR2dbcRepository, times(1)).findByPostId(postId);
    }
}
