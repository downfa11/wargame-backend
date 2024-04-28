package com.ns.wargame.Service;

import com.ns.wargame.Domain.GameResult;
import com.ns.wargame.Domain.User;
import com.ns.wargame.Domain.dto.Client;
import com.ns.wargame.Domain.dto.GameResultRequest;
import com.ns.wargame.Domain.dto.GameResultUpdate;
import com.ns.wargame.Repository.ResultR2dbcRepository;
import com.ns.wargame.Repository.UserR2dbcRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameResultService {
    private final ResultR2dbcRepository resultR2dbcRepository;
    private final UserR2dbcRepository userRepository;
    private final UserService userService;

    public Mono<Void> enroll(GameResultRequest request) {

        List<String> winTeamsAsString = new ArrayList<>();
        for (Client client : request.getWinTeams())
            winTeamsAsString.add(client.toString());

        List<String> loseTeamsAsString = new ArrayList<>();
        for (Client client : request.getLoseTeams())
            loseTeamsAsString.add(client.toString());

        GameResult result = GameResult.builder()
                .GameSpaceCode(request.getSpaceId())
                .winTeamString(request.getWinTeamString())
                .loseTeamString(request.getLoseTeamString())
                .winTeams(winTeamsAsString)
                .loseTeams(loseTeamsAsString)
                .dateTime(request.getDateTime())
                .gameDuration(request.getGameDuration())
                .createdAt(Timestamp.from(Instant.now()))
                .updatedAt(Timestamp.from(Instant.now()))
                .build();

        return resultR2dbcRepository.save(result)
                .thenMany(
                        Flux.fromIterable(result.getWinTeams()).concatWith(Flux.fromIterable(result.getLoseTeams()))
                                .flatMap(userId -> userService.findById(Long.parseLong(userId))
                                        .flatMap(user -> {
                                            user.setCurGameSpaceCode("");
                                            return userRepository.save(user).thenReturn(new GameResultUpdate(result.getWinTeams().contains(userId) ? "win" : "lose", user));
                                        })
                                ).collectList()
                                .flatMapMany(updates -> updateEloAndSaveUsers(updates, result.getWinTeamString()))
                ).then();
    }


    private Mono<Void> updateEloAndSaveUsers(List<GameResultUpdate> updates,String winTeam) {

        long redTeamElo = calculateOpposingTeamElo("red",updates);
        long blueTeamElo = calculateOpposingTeamElo("blue",updates);

        updates.forEach(update -> {
            User user = update.getUser();
            long newElo = calculateElo(user.getElo(), update.getUserTeam().equals("red") ? redTeamElo : blueTeamElo, update.getUserTeam().equals(winTeam));
            user.setElo(newElo);
            user.setCurGameSpaceCode("");
        });

        return Flux.fromIterable(updates)
                .flatMap(update -> userRepository.save(update.getUser()))
                .then();

    }

    private long calculateOpposingTeamElo(String curTeam, List<GameResultUpdate> updates) {
        return updates.stream()
                .filter(update -> !update.getUserTeam().equals(curTeam))
                .mapToLong(update -> update.getUser().getElo())
                .sum();
    }
    private long calculateElo(long elo, long opposingTeamElo, boolean isWinner) {
        final int K = 16;
        final double EA = 1.0 / (1.0 + Math.pow(10, (opposingTeamElo - elo) / 400.0));
        int SA = isWinner ? 1 : 0;
        return (long) (elo + K * (SA - EA));
    }

    public Mono<Void> dodge(GameResultRequest result) {

        List<Client> allTeams = new ArrayList<>();
        allTeams.addAll(result.getWinTeams());
        allTeams.addAll(result.getLoseTeams());

        if (allTeams.isEmpty()) {
            log.warn("Win and lose teams are empty!");
            return Mono.empty();
        }

        return Flux.fromIterable(allTeams)
                .flatMap(client -> {
                    return userService.findById((long) client.getClientindex());
                })
                .map(user -> {
                    user.setCurGameSpaceCode("");
                    return user;
                })
                .flatMap(user -> userRepository.save(user))
                .then();
    }

}
