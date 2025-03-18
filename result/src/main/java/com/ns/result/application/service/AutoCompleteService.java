package com.ns.result.application.service;

import com.ns.result.application.port.out.search.AutoCompletePlayerPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class AutoCompleteService {
    private final AutoCompletePlayerPort autoCompletePlayerPort;

    public Flux<String> getAutoCompleteSuggestions(String query){
        return autoCompletePlayerPort.getAutoCompleteSuggestions(query);
    }

}
