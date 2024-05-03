package com.ns.wargame.Domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table("results")
public class GameResult {
    @Id
    private Long id;
    private String code;
    private int channel;
    private int room;
    private String winTeam;
    private String loseTeam;

    private String dateTime;
    private int gameDuration;

}
