package com.ns.player.adapter.out.persistence;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@Builder
@Table("players")
public class Player {
    @Id
    private Long id;

    private String membershipId;
    private String aggregateIdentifier;
    private String nickname;
    private Long elo;
    private String code;

}
