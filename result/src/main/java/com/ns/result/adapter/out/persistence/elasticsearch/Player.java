package com.ns.result.adapter.out.persistence.elasticsearch;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.core.suggest.Completion;

@Getter
@Builder
@Document(indexName = "autocomplete_index")
public class Player {
    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "simple", searchAnalyzer = "simple")
    private String nickname;

    @Field(type = FieldType.Text, analyzer = "simple", searchAnalyzer = "simple")
    private Completion suggest;
}
