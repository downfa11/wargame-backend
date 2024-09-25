package com.ns.resultquery.dynamo;

import com.ns.resultquery.domain.ResultSumByChampName;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

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
}
