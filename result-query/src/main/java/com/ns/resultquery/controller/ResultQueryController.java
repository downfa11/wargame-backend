package com.ns.resultquery.controller;

import com.ns.resultquery.axon.query.ChampStat;
import com.ns.resultquery.service.ResultQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/result")
public class ResultQueryController {

    private final ResultQueryService resultQueryService;

    @GetMapping(path = "/query/champ/{champName}")
    Mono<Map<String, String>> getQueryToResultSumByChampName(@PathVariable String champName) {
        return resultQueryService.queryToResultSumByChampName(champName)
                .map(resultSum -> {
                    Map<String, String> result = new HashMap<>();

                    result.put("champName",resultSum.getChampName());
                    result.put("champCount", String.valueOf(resultSum.getChampCount()));
                    result.put("winCount", String.valueOf(resultSum.getWinCount()));
                    result.put("loseCount", String.valueOf(resultSum.getLoseCount()));

                    double percent = resultSum.getChampCount() > 0 ? (double) resultSum.getWinCount() / resultSum.getChampCount() * 100 : 0.0;
                    result.put("percent", String.format("%.1f", percent) + "%");

                    return result;
                })
                .onErrorResume(e -> {
                    return Mono.just(Collections.singletonMap("error", "Error retrieving data: " + e.getMessage()));
                });
    }

    @GetMapping(path = "/query/user/{userName}")
    Mono<Map<String, Object>> getQueryToResultSumByUserName(@PathVariable String userName) {
        return resultQueryService.queryToResultByUserName(userName)
                .map(resultSum -> {
                    Map<String, Object> result = new HashMap<>();

                    result.put("userName",resultSum.getUsername());
                    result.put("entireCount", String.valueOf(resultSum.getEntireCount()));
                    result.put("winCount", String.valueOf(resultSum.getWinCount()));
                    result.put("loseCount", String.valueOf(resultSum.getLoseCount()));

                    double percent = resultSum.getEntireCount() > 0 ? (double) resultSum.getWinCount() / resultSum.getEntireCount() * 100 : 0.0;
                    result.put("percent", String.format("%.1f", percent) + "%");

                    List<ChampStat> champStatList = resultSum.getChampStatList().stream()
                            .map(champStat -> {
                                double champWinRate = champStat.getResultCount() > 0 ?
                                        (double) champStat.getWinCount() / champStat.getResultCount() * 100 : 0.0;

                                return ChampStat.builder()
                                        .champIndex(champStat.getChampIndex())
                                        .champName(champStat.getChampName())
                                        .resultCount(champStat.getResultCount())
                                        .winCount(champStat.getWinCount())
                                        .loseCount(champStat.getLoseCount())
                                        .percent(String.format("%.1f", champWinRate) + "%")
                                        .build();
                            })
                            .collect(Collectors.toList());

                    result.put("champStatList", champStatList);


                    return result;
                })
                .onErrorResume(e -> {
                    return Mono.just(Collections.singletonMap("error", "Error retrieving data: " + e.getMessage()));
                });
    }
}