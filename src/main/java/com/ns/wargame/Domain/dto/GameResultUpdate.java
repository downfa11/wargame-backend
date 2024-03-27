package com.ns.wargame.Domain.dto;

import com.ns.wargame.Domain.User;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GameResultUpdate {
    private final String userTeam;
    private final User user;
}
