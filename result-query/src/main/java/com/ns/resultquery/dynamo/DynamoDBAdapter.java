package com.ns.resultquery.dynamo;

import com.ns.resultquery.axon.QueryResultSumByChampName;
import com.ns.resultquery.axon.query.CountSumByChamp;
import com.ns.resultquery.domain.ResultSumByChampName;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class DynamoDBAdapter {
    private static final String TABLE_NAME = "wargame-champ-query";
    private final DynamoDbClient dynamoDbClient;
    private final DynamoDBMapper dynamodbMapper;

    public DynamoDBAdapter(@Value("${dynamodb.accesskey}") String accessKey,
                           @Value("${dynamodb.secretkey}") String secretKey) {
        System.out.println("Auth DynamoDB : " + accessKey + ", " + secretKey);

        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        this.dynamoDbClient = DynamoDbClient.builder()
                .region(Region.AP_NORTHEAST_2)
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();

        this.dynamodbMapper = new DynamoDBMapper();
    }

    public Mono<Void> insertResultCountIncreaseEventByChampName(Long champIndex, String champName, Long season, Long resultCount, Long winCount, Long loseCount) {
        return Mono.fromRunnable(() -> {

            String datetime = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));

            // raw event insert (Insert, put)
            String pk = "#index:" + champIndex + "#name:" + champName + "#season:" + season + "#" + datetime;
            String sk = "-1";
            putItem(pk, sk, resultCount, winCount, loseCount);

            // 날짜별 판수 정보를 업데이트 (Query, Update)
            String summaryPk = pk + "#summary";
            String summarySk = "-1";
            ResultSumByChampName resultSumByChampName = getItem(summaryPk, summarySk);
            if (resultSumByChampName == null) {
                putItem(summaryPk, summarySk, resultCount, winCount, loseCount);
            } else{
                Long result = resultSumByChampName.getResultCount();
                result += resultCount;

                Long win = resultSumByChampName.getWinCount();
                win += winCount;

                Long lose = resultSumByChampName.getLoseCount();
                lose += loseCount;

                updateItem(summaryPk, summarySk, result, win, lose);
            }

            // 챔프별 정보
            String summaryPk2 = "#index:" + champIndex + "#name:" + champName + "#season:" + season;
            String summarySk2 = "-1";
            ResultSumByChampName resultSumByChampName2 = getItem(summaryPk2, summarySk2);
            if (resultSumByChampName2 == null) {
                putItem(summaryPk2, summarySk2, resultCount, winCount, loseCount);
            } else{
                Long result = resultSumByChampName.getResultCount();
                result += resultCount;

                Long win = resultSumByChampName.getWinCount();
                win += winCount;

                Long lose = resultSumByChampName.getLoseCount();
                lose += loseCount;

                updateItem(summaryPk2, summarySk2, result, win, lose);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    public Mono<ResultSumByChampName> getResultSumByChampName(String champName) {
        return Mono.fromCallable(() -> getItem(champName, "-1"))
                .subscribeOn(Schedulers.boundedElastic());
    }



    private void putItem(String pk, String sk, Long resultCount, Long winCount, Long loseCount) {
        try {
            String countStr = String.valueOf(resultCount);
            String winStr = String.valueOf(winCount);
            String loseStr = String.valueOf(loseCount);

            HashMap<String, AttributeValue> attrMap = new HashMap<>();
            attrMap.put("PK", AttributeValue.builder().s(pk).build());
            attrMap.put("SK", AttributeValue.builder().s(sk).build());

            attrMap.put("resultCount", AttributeValue.builder().n(countStr).build());
            attrMap.put("winCount", AttributeValue.builder().n(winStr).build());
            attrMap.put("loseCount", AttributeValue.builder().n(loseStr).build());

            PutItemRequest request = PutItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .item(attrMap)
                    .build();

            dynamoDbClient.putItem(request);
        } catch (DynamoDbException e) {
            System.err.println("Error adding an item to the table: " + e.getMessage());
        }
    }

    private ResultSumByChampName getItem(String pk, String sk) {
        try {
            HashMap<String, AttributeValue> attrMap = new HashMap<>();
            attrMap.put("PK", AttributeValue.builder().s(pk).build());
            attrMap.put("SK", AttributeValue.builder().s(sk).build());

            GetItemRequest request = GetItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .key(attrMap)
                    .build();

            GetItemResponse response = dynamoDbClient.getItem(request);
            if (response.hasItem()){
                return dynamodbMapper.mapToResultStatsByChampName(response.item());
            } else {
                return null;
            }

        } catch (DynamoDbException e) {
            System.err.println("Error getting an item from the table: " + e.getMessage());
        }
        return null;
    }


    private void queryItem(String id) {
        try {
            // PK 만 써도 돼요.
            HashMap<String, Condition> attrMap = new HashMap<>();
            attrMap.put("PK", Condition.builder()
                    .attributeValueList(AttributeValue.builder().s(id).build())
                    .comparisonOperator(ComparisonOperator.EQ)
                    .build());

            QueryRequest request = QueryRequest.builder()
                    .tableName(TABLE_NAME)
                    .keyConditions(attrMap)
                    .build();

            QueryResponse response = dynamoDbClient.query(request);
            response.items().forEach((value) -> System.out.println(value));
        } catch (DynamoDbException e) {
            System.err.println("Error getting an item from the table: " + e.getMessage());
        }
    }

    private void updateItem(String pk, String sk, Long resultCount, Long winCount, Long loseCount) {
        try {
            HashMap<String, AttributeValue> attrMap = new HashMap<>();
            attrMap.put("PK", AttributeValue.builder().s(pk).build());
            attrMap.put("SK", AttributeValue.builder().s(sk).build());

            String balanceStr = String.valueOf(resultCount);


            // Create an UpdateItemRequest
            UpdateItemRequest updateItemRequest = UpdateItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .key(attrMap)
                    .attributeUpdates(
                            new HashMap<String, AttributeValueUpdate>() {{
                                put("balance", AttributeValueUpdate.builder()
                                        .value(AttributeValue.builder().n(balanceStr).build())
                                        .action(AttributeAction.PUT)
                                        .build());
                            }}
                    ).build();


            UpdateItemResponse response = dynamoDbClient.updateItem(updateItemRequest);

            Map<String, AttributeValue> attributes = response.attributes();
            if (attributes != null) {
                for (Map.Entry<String, AttributeValue> entry : attributes.entrySet()) {
                    String attributeName = entry.getKey();
                    AttributeValue attributeValue = entry.getValue();
                    System.out.println(attributeName + ": " + attributeValue);
                }
            } else {
                System.out.println("Item was updated, but no attributes were returned.");
            }
        } catch (DynamoDbException e) {
            System.err.println("Error getting an item from the table: " + e.getMessage());
        }
    }

    @QueryHandler
    public Mono<CountSumByChamp> query(QueryResultSumByChampName resultSum) {
        String champName = resultSum.getChampName();

        return getResultSumByChampName(champName)
                .map(countSumByChamp -> CountSumByChamp.builder()
                        .CountSumByChampId(UUID.randomUUID().toString()) // todo. 해당 챔프의 Index 가져오기
                        .champName(champName)
                        .champCount(countSumByChamp.getResultCount())
                        .winCount(countSumByChamp.getWinCount())
                        .loseCount(countSumByChamp.getLoseCount())
                        .build());
    }
}
