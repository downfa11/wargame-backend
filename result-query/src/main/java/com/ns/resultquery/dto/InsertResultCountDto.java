package com.ns.resultquery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InsertResultCountDto {
    private Long champIndex;
    private String champName;
    private Long resultCount, winCount, loseCount;
    private Long champResult, champWin, champLose;

}
