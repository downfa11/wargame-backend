package com.ns.result.adapter.in.web;


import com.ns.result.adapter.out.persistence.elasticsearch.Result;
import com.ns.result.application.port.in.FindResultUseCase;
import com.ns.result.application.port.in.RegisterResultUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v1/result")
@Slf4j
@RequiredArgsConstructor
public class ResultController {

    private final RegisterResultUseCase registerResultUseCase;
    private final FindResultUseCase findResultUseCase;


    @GetMapping("/list")
    public Flux<Result> getResultList(){ return findResultUseCase.getResultList(); }

    @GetMapping("/search/name/{name}")
    public Flux<Result> getGameResultsByName(@PathVariable String name, @RequestParam int offset) {
        return findResultUseCase.getGameResultsByName(name, offset);
    }

    @GetMapping("/search/id/{membershipId}")
    public Flux<Result> getGameResultsByMembershipId(@PathVariable Long membershipId,  @RequestParam int offset) {
        return findResultUseCase.getGameResultsByMembershipId(membershipId, offset);
    }

    @PostMapping("/temp")
    public Mono<Result> createResultTemp(){
        return registerResultUseCase.createResultTemp();
    }

}

