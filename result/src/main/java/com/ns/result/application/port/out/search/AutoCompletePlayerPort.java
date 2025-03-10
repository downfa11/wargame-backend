package com.ns.result.application.port.out.search;

import reactor.core.publisher.Flux;

public interface AutoCompletePlayerPort {
    Flux<String> getAutoCompleteSuggestions(String query);
}
