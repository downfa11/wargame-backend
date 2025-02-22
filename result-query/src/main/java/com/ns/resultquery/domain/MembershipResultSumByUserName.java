package com.ns.resultquery.domain;

import com.ns.resultquery.adapter.axon.query.ChampStat;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
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
