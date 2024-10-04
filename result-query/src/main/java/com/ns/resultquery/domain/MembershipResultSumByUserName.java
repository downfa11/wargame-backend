package com.ns.resultquery.domain;

import com.ns.resultquery.axon.query.ChampStat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MembershipResultSumByUserName {
    private String PK;
    private String SK;
    private Long resultCount;
    private Long winCount;
    private Long loseCount;

    private List<ChampStat> champStatList;
}
