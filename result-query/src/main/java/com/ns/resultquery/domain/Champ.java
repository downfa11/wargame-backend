package com.ns.resultquery.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("champion_stats")
public class Champ {
    @Id
    private Integer championId;
    private String name;
}
