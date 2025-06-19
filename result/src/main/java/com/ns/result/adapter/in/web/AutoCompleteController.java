package com.ns.result.adapter.in.web;

import com.ns.result.application.service.AutoCompleteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/search")
public class AutoCompleteController {

    private final AutoCompleteService autoCompleteService;

    @GetMapping("/autocomplete")
    public Mono<List<String>> getSuggestions(@RequestParam String query) {
        if (query == null || query.trim().isEmpty()) {
            return Mono.just(Collections.emptyList());
        }

        return autoCompleteService.getAutoCompleteSuggestions(query.trim())
                .collectList()
                .flatMap(list -> Mono.just(list))
                .onErrorResume(e -> Mono.just(Collections.emptyList()));
    }
}
