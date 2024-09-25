package com.ns.result.domain.entity;

import com.ns.common.ClientRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(indexName ="results")
public class Result {
    @Id
    private String spaceId;
    private String state; // dodge: 비정상적인 상황, success: 정상적인 상황

    private int channel;
    private int room;
    private String winTeam;
    private String loseTeam;

    @Field(type = FieldType.Nested, includeInParent = true)
    private List<ClientRequest> blueTeams;
    @Field(type = FieldType.Nested, includeInParent = true)
    private List<ClientRequest> redTeams;

    private String dateTime;
    private int gameDuration;
}
