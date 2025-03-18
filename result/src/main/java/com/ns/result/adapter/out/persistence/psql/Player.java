package com.ns.result.adapter.out.persistence.psql;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@Table("players")
public class Player {
    @Id
    private Long id;

    private String membershipId;
    private String aggregateIdentifier;

    private Long elo;
    private String code;

}
