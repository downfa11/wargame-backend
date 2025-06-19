package com.ns.player.adapter.out.persistence;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

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
    private Tier tier;
    private Long elo;
    private String code;

    @Column("last_game_time")
    private LocalDateTime lastGameTime;

}
