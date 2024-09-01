package com.ns.result.controller;


import com.ns.result.domain.entity.Result;
import com.ns.result.service.ResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/game")
@Slf4j
@RequiredArgsConstructor
public class ResultController {

    private final ResultService resultService;

    @GetMapping("/elastic/search")
    public Flux<Result> getGameResultsByName(@RequestParam String name) {
        return resultService.getGameResultsByName(name);
    }

}

