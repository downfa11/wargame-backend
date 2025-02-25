package com.ns.resultquery.adapter.in.web;

import static com.ns.resultquery.exception.ErrorCode.RETRIEVE_DATA_ERROR_MESSAGE;

import com.ns.resultquery.adapter.axon.query.ChampStat;
import com.ns.resultquery.adapter.axon.query.CountSumByChamp;
import com.ns.resultquery.adapter.axon.query.CountSumByMembership;
import com.ns.resultquery.application.port.in.FindStatisticsUseCase;
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
@RequestMapping("/statistics")
public class ResultQueryController {

    private final FindStatisticsUseCase findStatisticsUseCase;

    @GetMapping(path = "/query/champ/{champName}")
    Mono<Map<String, String>> getQueryToResultSumByChampName(@PathVariable String champName) {
        return findStatisticsUseCase.findStatisticsByChampion(champName)
                .map(this::getResultSumByChampName)
                .onErrorResume(e -> Mono.just(Collections.singletonMap("error", RETRIEVE_DATA_ERROR_MESSAGE + e.getMessage())));
    }

    @GetMapping(path = "/query/user/{userName}")
    Mono<Map<String, Object>> getQueryToResultSumByUserName(@PathVariable String userName) {
        return findStatisticsUseCase.findStatisticsByUserName(userName)
                .map(this::getResultSumByUserName)
                .onErrorResume(e -> Mono.just(Collections.singletonMap("error", RETRIEVE_DATA_ERROR_MESSAGE + e.getMessage())));
    }


    private Map<String, String> getResultSumByChampName(CountSumByChamp resultSum){
        Map<String, String> result = new HashMap<>();

        result.put("champName",resultSum.getChampName());
        result.put("champCount", String.valueOf(resultSum.getChampCount()));
        result.put("winCount", String.valueOf(resultSum.getWinCount()));
        result.put("loseCount", String.valueOf(resultSum.getLoseCount()));
        result.put("percent", calcCountPercent(resultSum.getChampCount(), resultSum.getWinCount()));
        return result;
    }

    private Map<String, Object> getResultSumByUserName(CountSumByMembership resultSum){
        Map<String, Object> result = new HashMap<>();

        result.put("userName",resultSum.getUsername());
        result.put("entireCount", String.valueOf(resultSum.getEntireCount()));
        result.put("winCount", String.valueOf(resultSum.getWinCount()));
        result.put("loseCount", String.valueOf(resultSum.getLoseCount()));
        result.put("percent", calcCountPercent(resultSum.getEntireCount(), resultSum.getWinCount()));
        result.put("champStatList", getChampStatList(resultSum));

        return result;
    }

    private List<ChampStat> getChampStatList(CountSumByMembership resultSum){
        return resultSum.getChampStatList().stream()
                .map(this::createChampStat)
                .collect(Collectors.toList());
    }

    private ChampStat createChampStat(ChampStat champStat){
        return ChampStat.builder()
                .champIndex(champStat.getChampIndex())
                .champName(champStat.getChampName())
                .resultCount(champStat.getResultCount())
                .winCount(champStat.getWinCount())
                .loseCount(champStat.getLoseCount())
                .percent(calcCountPercent(champStat.getResultCount(), champStat.getWinCount()))
                .build();
    }

    private String calcCountPercent(Long entireCount, Long winCount){
        double percent = entireCount > 0 ? (double) winCount / entireCount * 100 : 0.0;
        return String.format("%.1f", percent) + "%";
    }
}