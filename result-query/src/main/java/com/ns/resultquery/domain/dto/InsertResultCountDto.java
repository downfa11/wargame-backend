package com.ns.resultquery.domain.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InsertResultCountDto {
    private Long champIndex;
    private String champName;
    private Long resultCount, winCount, loseCount;
    private Long champResult, champWin, champLose;
}
