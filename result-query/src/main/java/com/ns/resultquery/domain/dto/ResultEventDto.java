package com.ns.resultquery.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResultEventDto {
    private Long champIndex;
    private String champName;
    private Long resultCount;
    private Long winCount;
    private Long loseCount;
}
