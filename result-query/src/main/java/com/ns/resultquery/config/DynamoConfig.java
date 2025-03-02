package com.ns.resultquery.config;

import com.ns.resultquery.adapter.out.dynamo.DynamoDBAdapter;
import com.ns.resultquery.adapter.out.dynamo.DynamoDBMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Slf4j
@Configuration
public class DynamoConfig {

    @Value("${dynamodb.accesskey}")
    private String accessKey;

    @Value("${dynamodb.secretkey}")
    private String secretKey;

    @Bean
    public DynamoDbClient dynamoDbClient() {
        log.info("Auth DynamoDB : " + accessKey + ", " + secretKey);

        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        return DynamoDbClient.builder()
                .region(Region.AP_NORTHEAST_2)
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }

    @Bean
    public DynamoDBMapper dynamoDbMapper() {
        return new DynamoDBMapper();
    }
}
