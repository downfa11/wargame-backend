package com.ns.result.application.service;

import com.ns.common.anotation.UseCase;
import com.ns.result.application.port.out.search.AutoCompletePlayerPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@UseCase
@RequiredArgsConstructor
public class AutoCompleteService {
    private final AutoCompletePlayerPort autoCompletePlayerPort;

    public Flux<String> getAutoCompleteSuggestions(String query){
        return autoCompletePlayerPort.getAutoCompleteSuggestions(query);
    }

}
