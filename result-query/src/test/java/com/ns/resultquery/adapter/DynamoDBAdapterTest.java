package com.ns.resultquery.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ns.resultquery.adapter.axon.QueryResultSumByChampName;
import com.ns.resultquery.adapter.axon.QueryResultSumByUserName;
import com.ns.resultquery.adapter.axon.query.CountSumByChamp;
import com.ns.resultquery.adapter.axon.query.CountSumByMembership;
import com.ns.resultquery.adapter.out.dynamo.DynamoDBAdapter;
import com.ns.resultquery.adapter.out.dynamo.DynamoDBMapper;
import com.ns.resultquery.domain.MembershipResultSumByUserName;
import com.ns.resultquery.domain.ResultSumByChampName;
import com.ns.resultquery.domain.dto.InsertResultCountDto;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

@ExtendWith(MockitoExtension.class)
public class DynamoDBAdapterTest {

    private static final String CURRENT_SEASON = "2025";

    @Mock private DynamoDbClient dynamoDbClient;
    @Mock private DynamoDBMapper dynamoDBMapper;
    @InjectMocks private DynamoDBAdapter dynamoDBAdapter;


    @Test
    void 사용자의_이름으로_통계를_조회하는_경우() {
        // given
        String champName = "champ";
        String pk = champName + "_season" + CURRENT_SEASON;
        String sk = "-1";

        HashMap<String, AttributeValue> item = new HashMap<>();
        item.put("PK", AttributeValue.builder().s(pk).build());
        item.put("SK", AttributeValue.builder().s(sk).build());
        item.put("MembershipStat", AttributeValue.builder().s("SomeStats").build());

        GetItemResponse getItemResponse = GetItemResponse.builder()
                .item(item)
                .build();

        when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(getItemResponse);

        MembershipResultSumByUserName mockResult = new MembershipResultSumByUserName(champName, "100", 50L, 50L, 0L,
                List.of());
        when(dynamoDBMapper.mapToMembershipResultStatsByUserName(anyMap())).thenReturn(mockResult);

        // when
        MembershipResultSumByUserName result = dynamoDBAdapter.getMembershipResultSumByUserName(champName);

        // then
        assertNotNull(result);
        assertEquals(mockResult, result);
        verify(dynamoDbClient, times(1)).getItem(any(GetItemRequest.class));
    }

    @Test
    void 사용자의_이름으로_통계를_조회하는_경우_데이터를_찾을수_없음() {
        // given
        String champName = "champ";
        String pk = champName + "_season" + CURRENT_SEASON;
        String sk = "-1";

        GetItemResponse getItemResponse = GetItemResponse.builder()
                .item(new HashMap<>())
                .build();

        when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(getItemResponse);

        // when
        MembershipResultSumByUserName result = dynamoDBAdapter.getMembershipResultSumByUserName(champName);

        // then
        assertNull(result);
        verify(dynamoDbClient, times(1)).getItem(any(GetItemRequest.class));
    }

    @Test
    void 사용자의_이름으로_통계를_조회하는_경우_예외처리() {
        // given
        String champName = "champ";
        when(dynamoDbClient.getItem(any(GetItemRequest.class)))
                .thenThrow(DynamoDbException.builder().message("Error").build());

        // when
        MembershipResultSumByUserName result = dynamoDBAdapter.getMembershipResultSumByUserName(champName);

        // then
        assertNull(result);
        verify(dynamoDbClient, times(1)).getItem(any(GetItemRequest.class));
    }

    @Test
    void 챔프의_이름으로_통계를_조회하는_메서드() {
        // given

        String champName = "champ";
        String pk = champName + "_season" + CURRENT_SEASON;
        String sk = "-1";

        HashMap<String, AttributeValue> item = new HashMap<>();
        item.put("PK", AttributeValue.builder().s(pk).build());
        item.put("SK", AttributeValue.builder().s(sk).build());
        item.put("MembershipStat", AttributeValue.builder().s("SomeStats").build());

        GetItemResponse getItemResponse = GetItemResponse.builder()
                .item(item)
                .build();

        when(dynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(getItemResponse);

        ResultSumByChampName mockResult = new ResultSumByChampName(champName, "sort key", 100L, 50L, 50L);
        when(dynamoDBMapper.mapToResultStatsByChampName(anyMap())).thenReturn(mockResult);

        // when
        ResultSumByChampName result = dynamoDBAdapter.getResultSumByChampName(champName);

        // then
        assertNotNull(result);
        assertEquals(100L, result.getResultCount());
        assertEquals(50L, result.getWinCount());
        assertEquals(50L, result.getLoseCount());

        assertNotNull(result);
        assertEquals(mockResult, result);
        verify(dynamoDbClient, times(1)).getItem(any(GetItemRequest.class));
    }
}
