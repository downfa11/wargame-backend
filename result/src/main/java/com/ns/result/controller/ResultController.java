package com.ns.result.controller;


import com.ns.result.domain.entity.Result;
import com.ns.result.service.ResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/result")
@Slf4j
@RequiredArgsConstructor
public class ResultController {

    private final ResultService resultService;

    @GetMapping("/elastic/search")
    public Flux<Result> getGameResultsByName(@RequestParam String name) {
        return resultService.getGameResultsByName(name);
    }

    @PostMapping("/temp")
    public Mono<Result> createResultTemp(){
        return resultService.createResultTemp();
    }

    @GetMapping("/list")
    public Flux<Result> getResultList(){
        return resultService.getResultList();
    }

}

