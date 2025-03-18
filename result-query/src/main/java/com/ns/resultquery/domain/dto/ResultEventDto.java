package com.ns.resultquery.domain.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResultEventDto {
    private Long champIndex;
    private String champName;
    private Long resultCount;
    private Long winCount;
    private Long loseCount;
}
