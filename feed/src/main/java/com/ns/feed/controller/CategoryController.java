package com.ns.feed.controller;


import com.ns.common.messageEntity;
import com.ns.feed.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping("/v1/categorys")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @PostMapping("")
    public Mono<ResponseEntity<messageEntity>> register(@RequestParam String CategoryName){
        return categoryService.register(CategoryName)
                .map(category -> ResponseEntity.ok().body(new messageEntity("Success", category)))
                .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "category result is empty.")));
    }

    @PatchMapping("/{categoryId}")
    public Mono<ResponseEntity<messageEntity>> modify(@PathVariable Long categoryId, @RequestParam String CategoryName){
        return categoryService.modify(categoryId, CategoryName)
                .map(category -> ResponseEntity.ok().body(new messageEntity("Success", category)))
                .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "category result is empty.")));
    }

    @GetMapping("/{categoryId}")
    public Mono<ResponseEntity<messageEntity>> findCategoryById(@PathVariable Long categoryId){
        return categoryService.findById(categoryId)
                .map(category -> ResponseEntity.ok().body(new messageEntity("Success", category)))
                .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "category result is empty.")));
    }

    @DeleteMapping("/{categoryId}")
    public Mono<ResponseEntity<messageEntity>> deleteCategory(@PathVariable Long categoryId){
        return categoryService.deleteById(categoryId)
                .map(category -> ResponseEntity.ok().body(new messageEntity("Success", category)));
    }
}
