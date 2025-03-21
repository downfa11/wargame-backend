package com.ns.resultquery.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ResultSumByChampName {
    private String PK;
    private String SK;
    private Long resultCount;
    private Long winCount;
    private Long loseCount;
}
