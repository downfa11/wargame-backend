package com.ns.resultquery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MembershipResultEventDto {
    private Long membershipId;
    private String userName;
    private Long champIndex;
    private String champName;
    private Long resultCount;
    private Long winCount;
    private Long loseCount;
}
