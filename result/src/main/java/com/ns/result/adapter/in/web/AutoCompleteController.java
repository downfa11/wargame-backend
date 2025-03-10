package com.ns.result.adapter.in.web;

import com.ns.result.application.service.AutoCompleteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Flux;

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
    public Flux<String> getSuggestions(@RequestParam String query) {
        return autoCompleteService.getAutoCompleteSuggestions(query);
    }
}
