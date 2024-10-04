package com.ns.resultquery.dynamo;

import com.ns.resultquery.axon.QueryResultSumByChampName;
import com.ns.resultquery.axon.QueryResultSumByUserName;
import com.ns.resultquery.axon.query.ChampStat;
import com.ns.resultquery.axon.query.CountSumByChamp;
import com.ns.resultquery.axon.query.CountSumByMembership;
import com.ns.resultquery.domain.MembershipResultSumByUserName;
import com.ns.resultquery.domain.ResultSumByChampName;
import com.ns.resultquery.dto.InsertResultCountDto;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class DynamoDBAdapter {
    private static final String CHAMP_TABLE_NAME = "wargame-champ-query";
    private static final String MEMBERSHIP_TABLE_NAME = "wargame-membership-query";
    private static final String CURRENT_SEASON = "1";
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

    public Mono<Void> insertResultCountIncreaseEventByChampName(Long champIndex, String champName, Long resultCount, Long winCount, Long loseCount) {
        return Mono.fromRunnable(() -> {

            String datetime = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));

            // raw event insert (Insert, put)
            String pk = "#" + champIndex + "_" + champName + "_season" + CURRENT_SEASON + "_" + datetime;
            String sk = "-1";
            putResult(pk, sk, resultCount, winCount, loseCount);

            // 날짜별 판수 정보를 업데이트 (Query, Update)
            String summaryPk = pk + "#summary";
            String summarySk = "-1";
            ResultSumByChampName resultSumByChampName = getResult(summaryPk, summarySk);
            if (resultSumByChampName == null) {
                putResult(summaryPk, summarySk, resultCount, winCount, loseCount);
            } else{
                Long result = resultSumByChampName.getResultCount();
                result += resultCount;

                Long win = resultSumByChampName.getWinCount();
                win += winCount;

                Long lose = resultSumByChampName.getLoseCount();
                lose += loseCount;

                updateResult(summaryPk, summarySk, result, win, lose);
            }

            // 챔프별 정보
            String summaryPk2 = champName + "_season" + CURRENT_SEASON;
            String summarySk2 = "-1";
            ResultSumByChampName resultSumByChampName2 = getResult(summaryPk2, summarySk2);
            if (resultSumByChampName2 == null) {
                putResult(summaryPk2, summarySk2, resultCount, winCount, loseCount);
            } else{
                Long result = resultSumByChampName.getResultCount();
                result += resultCount;

                Long win = resultSumByChampName.getWinCount();
                win += winCount;

                Long lose = resultSumByChampName.getLoseCount();
                lose += loseCount;

                updateResult(summaryPk2, summarySk2, result, win, lose);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    private void putResult(String pk, String sk, Long resultCount, Long winCount, Long loseCount) {
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
                    .tableName(CHAMP_TABLE_NAME)
                    .item(attrMap)
                    .build();

            dynamoDbClient.putItem(request);
        } catch (DynamoDbException e) {
            System.err.println("Error adding an item to the table: " + e.getMessage());
        }
    }

    private ResultSumByChampName getResult(String pk, String sk) {
        try {
            HashMap<String, AttributeValue> attrMap = new HashMap<>();
            attrMap.put("PK", AttributeValue.builder().s(pk).build());
            attrMap.put("SK", AttributeValue.builder().s(sk).build());

            GetItemRequest request = GetItemRequest.builder()
                    .tableName(CHAMP_TABLE_NAME)
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

    private void updateResult(String pk, String sk, Long resultCount, Long winCount, Long loseCount) {
        try {
            HashMap<String, AttributeValue> attrMap = new HashMap<>();
            attrMap.put("PK", AttributeValue.builder().s(pk).build());
            attrMap.put("SK", AttributeValue.builder().s(sk).build());

            String resultStr = String.valueOf(resultCount);
            String winStr = String.valueOf(winCount);
            String loseStr = String.valueOf(loseCount);


            UpdateItemRequest updateItemRequest = UpdateItemRequest.builder()
                    .tableName(CHAMP_TABLE_NAME)
                    .key(attrMap)
                    .attributeUpdates(
                            new HashMap<String, AttributeValueUpdate>() {{
                                put("resultCount", AttributeValueUpdate.builder()
                                        .value(AttributeValue.builder().n(resultStr).build())
                                        .action(AttributeAction.PUT)
                                        .build());
                                put("winCount", AttributeValueUpdate.builder()
                                        .value(AttributeValue.builder().n(winStr).build())
                                        .action(AttributeAction.PUT)
                                        .build());
                                put("loseCount", AttributeValueUpdate.builder()
                                        .value(AttributeValue.builder().n(loseStr).build())
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

    private void queryItem(String id) {
        try {
            HashMap<String, Condition> attrMap = new HashMap<>();
            attrMap.put("PK", Condition.builder()
                    .attributeValueList(AttributeValue.builder().s(id).build())
                    .comparisonOperator(ComparisonOperator.EQ)
                    .build());

            QueryRequest request = QueryRequest.builder()
                    .tableName(CHAMP_TABLE_NAME)
                    .keyConditions(attrMap)
                    .build();

            QueryResponse response = dynamoDbClient.query(request);
            response.items().forEach((value) -> System.out.println(value));
        } catch (DynamoDbException e) {
            System.err.println("Error getting an item from the table: " + e.getMessage());
        }
    }

    @QueryHandler
    public CountSumByChamp queryToChamp(QueryResultSumByChampName resultSum) {
        String champName = resultSum.getChampName();
        ResultSumByChampName resultSumByChampName = getResultSumByChampName(champName);

        if (resultSumByChampName == null) {
            System.err.println("No data found for champion: " + champName);
        }

        return CountSumByChamp.builder()
                        .champName(champName)
                        .champCount(resultSumByChampName.getResultCount())
                        .winCount(resultSumByChampName.getWinCount())
                        .loseCount(resultSumByChampName.getLoseCount())
                        .build();
    }

    public ResultSumByChampName getResultSumByChampName(String champName) {
        String pk = champName + "_season" + CURRENT_SEASON;
        String sk = "-1";
        return getResult(pk, sk);
    }


    public Mono<Void> insertResultCountIncreaseEventByUserName(Long membershipId, String username, InsertResultCountDto insertResultCountDto) {
        System.out.println("page0 :" + membershipId);
        return Mono.fromRunnable(() -> {

            System.out.println("page1 :" + insertResultCountDto);

            String datetime = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));

            // raw event insert (Insert, put)
            String pk = "#" + membershipId + "_" + username + "_season" + CURRENT_SEASON + "_" + datetime;
            String sk = "-1";
            putMembershipResult(pk, sk, insertResultCountDto);

            // 날짜별 판수 정보를 업데이트 (Query, Update)
            String summaryPk = pk + "#summary";
            String summarySk = "-1";
            MembershipResultSumByUserName membershipResult = getMembershipResult(summaryPk, summarySk);
            updateResult(membershipResult, summaryPk, summarySk, insertResultCountDto);

            System.out.println("page2 :" + insertResultCountDto);

            // 챔프별 정보
            String summaryPk2 = username + "_season" + CURRENT_SEASON;
            String summarySk2 = "-1";
            MembershipResultSumByUserName membershipResult2 = getMembershipResult(summaryPk2, summarySk2);
            updateResult(membershipResult2, summaryPk2, summarySk2, insertResultCountDto);

        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    private void updateResult(MembershipResultSumByUserName membershipResult, String pk, String sk, InsertResultCountDto insertResultCountDto) {
        if (membershipResult == null) {
            putMembershipResult(pk, sk, insertResultCountDto);
        } else {
            Long result = membershipResult.getResultCount() + insertResultCountDto.getResultCount();
            Long win = membershipResult.getWinCount() + insertResultCountDto.getWinCount();
            Long lose = membershipResult.getLoseCount() + insertResultCountDto.getLoseCount();

            ChampStat existingChampStat = membershipResult.getChampStatList().stream()
                    .filter(champStat -> champStat.getChampIndex().equals(insertResultCountDto.getChampIndex()))
                    .findFirst()
                    .orElse(null);

            if (existingChampStat != null) {
                Long champResult = existingChampStat.getResultCount() + insertResultCountDto.getResultCount();
                Long champWin = existingChampStat.getWinCount() + insertResultCountDto.getWinCount();
                Long champLose = existingChampStat.getLoseCount() + insertResultCountDto.getLoseCount();

                updateMembershipResult(pk, sk, result, win, lose, insertResultCountDto.getChampIndex(), insertResultCountDto.getChampName(), champResult, champWin, champLose);
            }
        }
    }

    @QueryHandler
    public CountSumByMembership queryToMembership(QueryResultSumByUserName resultSum) {
        String userName = resultSum.getUserName();
        MembershipResultSumByUserName resultSumByUserName = getMembershipResultSumByUserName(userName);

        if (resultSumByUserName == null) {
            System.err.println("No data found for membership: " + userName);
        }

        System.out.println("test : "+ resultSumByUserName);

        return CountSumByMembership.builder()
                .username(userName)
                .entireCount(resultSumByUserName.getResultCount())
                .winCount(resultSumByUserName.getWinCount())
                .loseCount(resultSumByUserName.getLoseCount())
                .champStatList(resultSumByUserName.getChampStatList())
                .build();
    }

    public MembershipResultSumByUserName getMembershipResultSumByUserName(String champName) {
        String pk = champName + "_season" + CURRENT_SEASON;
        String sk = "-1";
        return getMembershipResult(pk, sk);
    }

    private MembershipResultSumByUserName getMembershipResult(String pk, String sk) {
        try {
            HashMap<String, AttributeValue> attrMap = new HashMap<>();
            attrMap.put("PK", AttributeValue.builder().s(pk).build());
            attrMap.put("SK", AttributeValue.builder().s(sk).build());

            GetItemRequest request = GetItemRequest.builder()
                    .tableName(MEMBERSHIP_TABLE_NAME)
                    .key(attrMap)
                    .build();

            GetItemResponse response = dynamoDbClient.getItem(request);
            if (response.hasItem()){
                return dynamodbMapper.mapToMembershipResultStatsByUserName(response.item());
            } else {
                return null;
            }

        } catch (DynamoDbException e) {
            System.err.println("Error getting an item from the table: " + e.getMessage());
        }

        return null;
    }

    private void putMembershipResult(String pk, String sk, InsertResultCountDto insertResultCountDto) {
        try {
            String countStr = String.valueOf(insertResultCountDto.getResultCount());
            String winStr = String.valueOf(insertResultCountDto.getWinCount());
            String loseStr = String.valueOf(insertResultCountDto.getLoseCount());

            HashMap<String, AttributeValue> attrMap = new HashMap<>();
            attrMap.put("PK", AttributeValue.builder().s(pk).build());
            attrMap.put("SK", AttributeValue.builder().s(sk).build());

            attrMap.put("resultCount", AttributeValue.builder().n(countStr).build());
            attrMap.put("winCount", AttributeValue.builder().n(winStr).build());
            attrMap.put("loseCount", AttributeValue.builder().n(loseStr).build());

            Map<String, AttributeValue> champStatMap = new HashMap<>();
            champStatMap.put("champIndex", AttributeValue.builder().n(String.valueOf(insertResultCountDto.getChampIndex())).build());
            champStatMap.put("champName", AttributeValue.builder().s(insertResultCountDto.getChampName()).build());
            champStatMap.put("champResult", AttributeValue.builder().n(String.valueOf(insertResultCountDto.getChampResult())).build());
            champStatMap.put("champWin", AttributeValue.builder().n(String.valueOf(insertResultCountDto.getChampWin())).build());
            champStatMap.put("champLose", AttributeValue.builder().n(String.valueOf(insertResultCountDto.getChampLose())).build());

            attrMap.put("champStatList", AttributeValue.builder().m(champStatMap).build());


            PutItemRequest request = PutItemRequest.builder()
                    .tableName(MEMBERSHIP_TABLE_NAME)
                    .item(attrMap)
                    .build();

            dynamoDbClient.putItem(request);
        } catch (DynamoDbException e) {
            System.err.println("Error adding an item to the table: " + e.getMessage());
        }
    }

    private void updateMembershipResult(String pk, String sk, Long result, Long win, Long lose, Long champIndex, String champName, Long champResult, Long champWin, Long champLose) {
        try {
            HashMap<String, AttributeValue> attrMap = new HashMap<>();
            attrMap.put("PK", AttributeValue.builder().s(pk).build());
            attrMap.put("SK", AttributeValue.builder().s(sk).build());

            String resultStr = String.valueOf(result);
            String winStr = String.valueOf(win);
            String loseStr = String.valueOf(lose);

            Map<String, AttributeValue> champStatMap = new HashMap<>();
            champStatMap.put("champIndex", AttributeValue.builder().n(String.valueOf(champIndex)).build());
            champStatMap.put("champName", AttributeValue.builder().s(champName).build());
            champStatMap.put("champResult", AttributeValue.builder().n(String.valueOf(champResult)).build());
            champStatMap.put("champWin", AttributeValue.builder().n(String.valueOf(champWin)).build());
            champStatMap.put("champLose", AttributeValue.builder().n(String.valueOf(champLose)).build());

            UpdateItemRequest updateItemRequest = UpdateItemRequest.builder()
                    .tableName(MEMBERSHIP_TABLE_NAME)
                    .key(attrMap)
                    .attributeUpdates(
                            new HashMap<String, AttributeValueUpdate>() {{
                                put("resultCount", AttributeValueUpdate.builder()
                                        .value(AttributeValue.builder().n(resultStr).build())
                                        .action(AttributeAction.PUT)
                                        .build());
                                put("winCount", AttributeValueUpdate.builder()
                                        .value(AttributeValue.builder().n(winStr).build())
                                        .action(AttributeAction.PUT)
                                        .build());
                                put("loseCount", AttributeValueUpdate.builder()
                                        .value(AttributeValue.builder().n(loseStr).build())
                                        .action(AttributeAction.PUT)
                                        .build());
                                put("champStatList", AttributeValueUpdate.builder()
                                        .value(AttributeValue.builder().m(champStatMap).build())
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


}
