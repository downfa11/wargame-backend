package com.ns.feed.application.service;


import com.ns.feed.adapter.out.persistence.Category;
import com.ns.feed.adapter.out.persistence.CategoryR2dbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryR2dbcRepository categoryR2dbcRepository;

    public Mono<Category> register(String categoryName){
        return categoryR2dbcRepository.save(Category.builder()
                        .CategoryName(categoryName)
                        .build());
    }

    public Mono<Category> modify(Long categoryId,String categoryName){
        return categoryR2dbcRepository.findById(categoryId)
                .flatMap(category -> {
                    category.setCategoryName(categoryName);
                    return categoryR2dbcRepository.save(category);
                });
    }


    public Mono<Category> findById(Long id){
        return categoryR2dbcRepository.findById(id);
    }

    public Mono<Void> deleteById(Long id){
        return categoryR2dbcRepository.deleteById(id);
    }
}
