package com.ns.result.adapter.in.web;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/opensearch")
@RequiredArgsConstructor
public class OpenSearchController {

//    private final OpenSearchService openSearchService;
//
//    @GetMapping("/check-field-existence/{fieldName}")
//    public Mono<List<JsonNode>> checkFieldExistence(@PathVariable String fieldName) {
//        return openSearchService.checkFieldExistence(fieldName);
//    }
//
//    @PostMapping("/query")
//    public ResponseEntity<Flux<JsonNode>> executeQueryStringSearch(@RequestBody String queryString) {
//        Flux<JsonNode> results = openSearchService.executeQueryStringSearch(queryString);
//        return ResponseEntity.ok(results);
//    }
//
//    @PostMapping("/conditional")
//    public ResponseEntity<Flux<JsonNode>> executeConditionalSearch(@RequestBody SearchRequestTemplate searchRequest, @RequestParam(defaultValue = "10") int size) {
//        Flux<JsonNode> results = openSearchService.executeConditionalSearch(searchRequest, size);
//        return ResponseEntity.ok(results);
//    }
}
