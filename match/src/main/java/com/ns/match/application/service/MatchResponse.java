package com.ns.match.application.service;


import static com.ns.match.adapter.out.RedisMatchProcessAdapter.MAX_ALLOW_USER_COUNT;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MatchResponse {
    private String spaceId;
    private Map<String, List<String>> teams;

    public static MatchResponse fromMembers(String spaceId, List<String> members) {
        if(members.size()!=MAX_ALLOW_USER_COUNT) return null;

        Collections.shuffle(members);

        List<String> blueTeam = members.subList(0, members.size()/2);
        List<String> redTeam = members.subList(members.size()/2, members.size());

        Map<String, List<String>> teams = new HashMap<>();
        teams.put("blue", blueTeam);
        teams.put("red", redTeam);

        return MatchResponse.builder()
                .spaceId(spaceId)
                .teams(teams)
                .build();
    }
}
