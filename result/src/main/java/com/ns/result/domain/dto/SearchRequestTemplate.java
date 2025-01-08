package com.ns.result.domain.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchRequestTemplate {
    private String newIndex;
    private String logGroup;
    private String startDate;
    private String endDate;
    private String whereClause;
    private List<String> fields;
}

//        {
//            "newIndex": "cwl-*",
//                "logGroup": "aws-waf-logs-groups",
//                "startDate": "2024-08-03T17:36:21.164",
//                "endDate": "2024-08-04T23:43:09.545",
//                "whereClause": "string",
//                "fields": [ "@timestamp","@message" ]
//        }