package com.ns.result.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.ns.result.domain.dto.SearchRequestTemplate;
import com.ns.result.service.OpenSearchService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/opensearch")
@RequiredArgsConstructor
public class OpenSearchController {

    private final OpenSearchService openSearchService;

    @GetMapping("/check-field-existence/{fieldName}")
    public Mono<List<JsonNode>> checkFieldExistence(@PathVariable String fieldName) {
        return openSearchService.checkFieldExistence(fieldName);
    }

    @PostMapping("/query")
    public ResponseEntity<Flux<JsonNode>> executeQueryStringSearch(@RequestBody String queryString) {
        Flux<JsonNode> results = openSearchService.executeQueryStringSearch(queryString);
        return ResponseEntity.ok(results);
    }

    @PostMapping("/conditional")
    public ResponseEntity<Flux<JsonNode>> executeConditionalSearch(@RequestBody SearchRequestTemplate searchRequest, @RequestParam(defaultValue = "10") int size) {
        Flux<JsonNode> results = openSearchService.executeConditionalSearch(searchRequest, size);
        return ResponseEntity.ok(results);
    }
}
