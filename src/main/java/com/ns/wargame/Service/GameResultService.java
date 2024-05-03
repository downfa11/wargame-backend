package com.ns.wargame.Service;

import com.ns.wargame.Domain.GameResult;
import com.ns.wargame.Domain.User;
import com.ns.wargame.Domain.Client;
import com.ns.wargame.Domain.dto.ClientRequest;
import com.ns.wargame.Domain.dto.GameResultRequest;
import com.ns.wargame.Domain.dto.GameResultUpdate;
import com.ns.wargame.Repository.ClientR2dbcRepository;
import com.ns.wargame.Repository.ResultR2dbcRepository;
import com.ns.wargame.Repository.UserR2dbcRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveZSetOperations;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameResultService {
    private final ResultR2dbcRepository resultR2dbcRepository;
    private final UserR2dbcRepository userRepository;
    private final ClientR2dbcRepository clientRepository;
    private final ReactiveRedisTemplate<String, Long> reactiveRedisTemplate;
    private final UserService userService;
    public Mono<Void> updateRanking(Long userId) {
        return userService.findById(userId)
                .flatMap(user -> {
                    return reactiveRedisTemplate.opsForZSet().reverseRange("leaderboard", Range.closed(0L, 9L))
                            .collectList()
                            .flatMap(existingUsers -> {
                                if (existingUsers.size() < 10)
                                    return reactiveRedisTemplate.opsForZSet().add("leaderboard", userId, user.getElo());

                                else {
                                    Long lowestEloUser = existingUsers.get(existingUsers.size() - 1);
                                    return reactiveRedisTemplate.opsForZSet().score("leaderboard", lowestEloUser)
                                            .flatMap(lowestElo -> {
                                                // 점수가 동일한 경우 더 최신에 올라온 사람이 밀어낸다.
                                                if (user.getElo() >= lowestElo) {
                                                    return reactiveRedisTemplate.opsForZSet().remove("leaderboard", lowestEloUser)
                                                            .then(reactiveRedisTemplate.opsForZSet().add("leaderboard", userId, user.getElo()))
                                                            .onErrorResume(throwable -> {
                                                                log.error("Failed to update leaderboard: {}", throwable.getMessage());
                                                                return Mono.empty();
                                                            });
                                                }
                                                else
                                                    return Mono.empty();

                                            });
                                }
                            });
                })
                .then();
    }


    public Flux<Map<String, Object>> getLeaderboard() {
        ReactiveZSetOperations<String, Long> zSetOps = reactiveRedisTemplate.opsForZSet();
        return zSetOps.rangeWithScores("leaderboard", Range.closed(0L, 99L))
                .flatMapSequential(typedTuple -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("user ID", typedTuple.getValue());
                    data.put("score", typedTuple.getScore());
                    return Flux.just(data);
                });
    }

    public Mono<Void> enroll(GameResultRequest request) {
        List<ClientRequest> allTeamMembers = new ArrayList<>();
        allTeamMembers.addAll(request.getBlueTeams());
        allTeamMembers.addAll(request.getRedTeams());

        return Flux.fromIterable(allTeamMembers)
                .flatMap(member -> userService.findById((long) member.getClientindex())
                        .flatMap(user -> Mono.just(new GameResultUpdate(member.getTeam(), user)))
                        .onErrorResume(e -> {
                            log.error("User not found for id: " + member.getClientindex(), e);
                            return Mono.empty();
                        })
                        .switchIfEmpty(Mono.defer(() -> {
                            log.error("User not found for id: " + member.getClientindex());
                            return Mono.empty();
                        }))
                )
                .collectList()
                .flatMap(list -> {
                    // 여기에서 팀에 따라 분류된 결과를 가지고 ELO를 업데이트하고 사용자 정보를 저장하십시오.
                    // 예를 들어, winTeam에 대해 ELO를 증가시키고, loseTeam에 대해 감소 등의 처리를 할 수 있습니다.
                    // 이 부분의 구현은 프로젝트의 나머지 로직에 따라 달라질 수 있습니다.
                    return updateEloAndSaveUsers(request.getWinTeam(), list);
                })
                .then(saveGameResult(request));
    }





    private Client mapToClient(ClientRequest clientRequest, Long resultId) {

        String inventoryAsString = convertItemsToString(clientRequest.getItemList());

        return Client.builder()
                .gameResultId(resultId)
                .socket(clientRequest.getSocket())
                .userId((long) clientRequest.getClientindex())
                .champ(clientRequest.getChampindex())
                .name(clientRequest.getUser_name())
                .team(clientRequest.getTeam())
                .channel(clientRequest.getChannel())
                .room(clientRequest.getRoom())
                .kills(clientRequest.getKill())
                .deaths(clientRequest.getDeath())
                .assists(clientRequest.getAssist())
                .gold(clientRequest.getGold())
                .level(clientRequest.getLevel())
                .maxhp(clientRequest.getMaxhp())
                .maxmana(clientRequest.getMaxmana())
                .attack(clientRequest.getAttack())
                .critical(clientRequest.getCritical())
                .criProbability(clientRequest.getCriProbability())
                .attrange(clientRequest.getAttrange())
                .attspeed(clientRequest.getAttspeed())
                .movespeed(clientRequest.getMovespeed())
                .itemList(inventoryAsString)
                .build();
    }

    private String convertItemsToString(List<Integer> itemList){
        String itemListAsString = itemList.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",", "[", "]"));
        return itemListAsString;
    }

    private Mono<Void> updateEloAndSaveUsers(String winTeam, List<GameResultUpdate> updates) {

        long blueTeamElo = calculateOpposingTeamElo("red",updates); // red팀의 상대 팀(blue)의 전체 elo
        long redTeamElo = calculateOpposingTeamElo("blue",updates); // blue팀의 상대 팀(red)의 전체 elo

        log.info("redTeam elo : "+redTeamElo);
        log.info("blueTeam elo : "+blueTeamElo);

        return Flux.fromIterable(updates)
                .flatMap(update -> {
                    User user = update.getUser();
                    log.info("opposingTeamElo : "+ (update.getUserTeam().equals("red") ? blueTeamElo : redTeamElo));
                    log.info("win boolean : "+ update.getUserTeam().equals(winTeam));
                    long newElo = calculateElo(user.getElo(), update.getUserTeam().equals("red") ? blueTeamElo : redTeamElo, update.getUserTeam().equals(winTeam));
                    log.info(user.getName()+"님은 elo "+user.getElo()+"에서 elo "+newElo+"으로 변동이 생겼습니다.");
                    user.setElo(newElo);
                    user.setCurGameSpaceCode("");
                    return userRepository.save(user)
                            .doOnSuccess(v -> updateRanking(user.getId()));
                })
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

    public Mono<Void> saveGameResult(GameResultRequest request) {
        GameResult result = GameResult.builder()
                .code(request.getSpaceId())
                .channel(request.getChannel())
                .room(request.getRoom())
                .winTeam(request.getWinTeam())
                .loseTeam(request.getLoseTeam())
                .dateTime(request.getDateTime())
                .gameDuration(request.getGameDuration())
                .build();

        return resultR2dbcRepository.save(result)
                .thenMany(Flux.fromIterable(request.getBlueTeams())
                        .concatWith(Flux.fromIterable(request.getRedTeams()))
                        .flatMap(clientRequest -> {
                            Client client = mapToClient(clientRequest, result.getId());
                            return clientRepository.save(client);
                        }))
                .then();
    }

    public Mono<Void> dodge(GameResultRequest result) {

        List<ClientRequest> allTeams = new ArrayList<>();
        allTeams.addAll(result.getBlueTeams());
        allTeams.addAll(result.getRedTeams());

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
