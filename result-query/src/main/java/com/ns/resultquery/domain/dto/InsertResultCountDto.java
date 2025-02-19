package com.ns.resultquery.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InsertResultCountDto {
    private Long champIndex;
    private String champName;
    private Long resultCount, winCount, loseCount;
    private Long champResult, champWin, champLose;

}
