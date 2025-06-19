package com.ns.resultquery.adapter.in.web;

import static com.ns.resultquery.exception.ErrorCode.RETRIEVE_DATA_ERROR_MESSAGE;

import com.ns.resultquery.adapter.axon.query.ChampStat;
import com.ns.resultquery.adapter.axon.query.CountSumByChamp;
import com.ns.resultquery.adapter.axon.query.CountSumByMembership;
import com.ns.resultquery.application.port.in.FindStatisticsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/statistics")
public class ResultQueryController {

    private final FindStatisticsUseCase findStatisticsUseCase;

    @GetMapping(path = "/query/champ/{champName}")
    public ResponseEntity<Map<String, String>> getQueryToResultSumByChampName(@PathVariable String champName) {
        try {
            CountSumByChamp resultSum = findStatisticsUseCase.findStatisticsByChampion(champName);
            return ResponseEntity.ok(getResultSumByChampName(resultSum));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", RETRIEVE_DATA_ERROR_MESSAGE + e.getMessage()));
        }
    }

    @GetMapping(path = "/query/champs")
    public ResponseEntity<List<Map<String, String>>> getQueryToResultSum() {
        try {
            List<CountSumByChamp> resultSum = findStatisticsUseCase.findStatisticsByAllChampionInCurrentSeason();
            List<Map<String, String>> body = resultSum.stream()
                    .map(this::getResultSumByChampName)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            Map<String, String> errorBody = Map.of("error", RETRIEVE_DATA_ERROR_MESSAGE + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of(errorBody));
        }
    }

    @GetMapping(path = "/query/user/{userName}")
    public ResponseEntity<Map<String, Object>> getQueryToResultSumByUserName(@PathVariable String userName) {
        try {
            CountSumByMembership resultSum = findStatisticsUseCase.findStatisticsByUserName(userName);
            return ResponseEntity.ok(getResultSumByUserName(resultSum));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", RETRIEVE_DATA_ERROR_MESSAGE + e.getMessage()));
        }
    }

    private Map<String, String> getResultSumByChampName(CountSumByChamp resultSum){
        Map<String, String> result = new HashMap<>();
        result.put("champName", resultSum.getChampName());
        result.put("champCount", String.valueOf(resultSum.getChampCount()));
        result.put("winCount", String.valueOf(resultSum.getWinCount()));
        result.put("loseCount", String.valueOf(resultSum.getLoseCount()));
        result.put("percent", calcCountPercent(resultSum.getChampCount(), resultSum.getWinCount()));
        return result;
    }

    private Map<String, Object> getResultSumByUserName(CountSumByMembership resultSum){
        Map<String, Object> result = new HashMap<>();
        result.put("userName", resultSum.getUsername());
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
        return String.format("%.1f", percent);
    }
    
    
}