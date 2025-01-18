package com.ns.feed.controller;


import com.ns.common.utils.MessageEntity;
import com.ns.feed.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping("/v1/categorys")
@RequiredArgsConstructor
public class CategoryController {
    private final String CATEGROY_RESULT_EMPTY_ERROR_MESSAGE = "category result is empty.";

    private final CategoryService categoryService;

    @PostMapping("")
    public Mono<ResponseEntity<MessageEntity>> register(@RequestParam String CategoryName){
        return categoryService.register(CategoryName)
                .map(category -> ResponseEntity.ok().body(new MessageEntity("Success", category)))
                .defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", CATEGROY_RESULT_EMPTY_ERROR_MESSAGE)));
    }

    @PatchMapping("/{categoryId}")
    public Mono<ResponseEntity<MessageEntity>> modify(@PathVariable Long categoryId, @RequestParam String CategoryName){
        return categoryService.modify(categoryId, CategoryName)
                .map(category -> ResponseEntity.ok().body(new MessageEntity("Success", category)))
                .defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", CATEGROY_RESULT_EMPTY_ERROR_MESSAGE)));
    }

    @GetMapping("/{categoryId}")
    public Mono<ResponseEntity<MessageEntity>> findCategoryById(@PathVariable Long categoryId){
        return categoryService.findById(categoryId)
                .map(category -> ResponseEntity.ok().body(new MessageEntity("Success", category)))
                .defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", CATEGROY_RESULT_EMPTY_ERROR_MESSAGE)));
    }

    @DeleteMapping("/{categoryId}")
    public Mono<ResponseEntity<MessageEntity>> deleteCategory(@PathVariable Long categoryId){
        return categoryService.deleteById(categoryId)
                .map(category -> ResponseEntity.ok().body(new MessageEntity("Success", category)));
    }
}
