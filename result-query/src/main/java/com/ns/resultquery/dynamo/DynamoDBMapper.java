package com.ns.resultquery.dynamo;

import com.ns.resultquery.axon.query.ChampStat;
import com.ns.resultquery.domain.MembershipResultSumByUserName;
import com.ns.resultquery.domain.ResultSumByChampName;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DynamoDBMapper {
    public ResultSumByChampName mapToResultStatsByChampName(Map<String, AttributeValue> item) {
        return new ResultSumByChampName(
                item.get("PK").s(),
                item.get("SK").s(),
                Long.parseLong(item.get("resultCount").n()),
                Long.parseLong(item.get("winCount").n()),
                Long.parseLong(item.get("loseCount").n())
        );
    }

    public MembershipResultSumByUserName mapToMembershipResultStatsByUserName(Map<String, AttributeValue> item) {
        List<ChampStat> champStatList = new ArrayList<>();

        if (item.containsKey("champStatList")) {

            Map<String, AttributeValue> champStatMap = item.get("champStatList").m();

            Long champIndex = Long.parseLong(champStatMap.get("champIndex").n());
            String champName = champStatMap.get("champName").s();
            Long resultCount = Long.parseLong(champStatMap.get("champResult").n());
            Long winCount = Long.parseLong(champStatMap.get("champWin").n());
            Long loseCount = Long.parseLong(champStatMap.get("champLose").n());

            ChampStat champStat = ChampStat.builder()
                    .champIndex(champIndex)
                    .champName(champName)
                    .resultCount(resultCount)
                    .winCount(winCount)
                    .loseCount(loseCount)
                    .build();

                champStatList.add(champStat);
        }

        return new MembershipResultSumByUserName(
                item.get("PK").s(),
                item.get("SK").s(),
                Long.parseLong(item.get("resultCount").n()),
                Long.parseLong(item.get("winCount").n()),
                Long.parseLong(item.get("loseCount").n()),
                champStatList
        );
    }
}
