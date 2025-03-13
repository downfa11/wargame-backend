package com.ns.result.adapter.in.web;

import com.ns.result.application.service.AutoCompleteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AutoCompleteController {

    private final AutoCompleteService autoCompleteService;

    @GetMapping
    public Rendering tempPage() {
        return Rendering.view("temp").build();
    }

    @ResponseBody
    @GetMapping("/autocomplete")
    public Mono<List<String>> getSuggestions(@RequestParam String query) {
        return autoCompleteService.getAutoCompleteSuggestions(query)
                .collectList();
    }
}
