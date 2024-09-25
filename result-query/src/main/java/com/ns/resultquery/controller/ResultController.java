package com.ns.resultquery.controller;

import com.ns.resultquery.service.ResultQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/result")
public class ResultController {

    private final ResultQueryService resultQueryService;

    @GetMapping(path = "/query/{champName}")
    Mono<Map<String, String>> getQueryToResultSumByChampName(@PathVariable String champName) {
        return resultQueryService.queryToResultSumByChampName(champName)
                .flatMap(resultSum -> {
                    Map<String, String> result = new HashMap<>();
                    result.put("countSumByChampId",resultSum.getCountSumByChampId());
                    result.put("champName",resultSum.getChampName());
                    result.put("champCount", String.valueOf(resultSum.getChampCount()));
                    result.put("winCount", String.valueOf(resultSum.getWinCount()));
                    result.put("loseCount", String.valueOf(resultSum.getLoseCount()));
                    result.put("percent", String.valueOf(resultSum.getPercent()));
                    return Mono.just(result);
                });
    }
}