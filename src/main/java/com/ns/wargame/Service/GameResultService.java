package com.ns.wargame.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ns.wargame.Domain.GameResult;
import com.ns.wargame.Domain.GameResultDocument;
import com.ns.wargame.Domain.User;
import com.ns.wargame.Domain.Client;
import com.ns.wargame.Domain.dto.ClientRequest;
import com.ns.wargame.Domain.dto.GameResultRequest;
import com.ns.wargame.Domain.dto.GameResultUpdate;
import com.ns.wargame.Repository.ClientR2dbcRepository;
import com.ns.wargame.Repository.ResultR2dbcRepository;
import com.ns.wargame.Repository.ResultRepository;
import com.ns.wargame.Repository.UserR2dbcRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveZSetOperations;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameResultService {

    private final String GAME_RESULT_USER_KEY = "users:result:%s";
    private final Long expireTime = 60L;

    private final ResultR2dbcRepository resultR2dbcRepository;
    private final ResultRepository resultRepository;
    private final UserR2dbcRepository userRepository;
    private final ClientR2dbcRepository clientRepository;

    private final ReactiveRedisTemplate<String, Long> reactiveRedisTemplate;
    private final ReactiveRedisTemplate<String, String> RedisTemplateUserResult;

    private final ObjectMapper objectMapper;
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
        return saveResultToDatabase(request)
                .then(saveResultToElasticsearch(request))
                .then();
    }

    private Mono<Void> saveResultToDatabase(GameResultRequest request) {
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

    private Mono<GameResultDocument> saveResultToElasticsearch(GameResultRequest request) {
        GameResultDocument document = mapToGameResultDocument(request);
        return resultRepository.save(document);
    }

    private GameResultDocument mapToGameResultDocument(GameResultRequest request) {
        return GameResultDocument.builder()
                .spaceId(request.getSpaceId())
                .state("success")
                .channel(request.getChannel())
                .room(request.getRoom())
                .winTeam(request.getWinTeam())
                .loseTeam(request.getLoseTeam())
                .blueTeams(request.getBlueTeams())
                .redTeams(request.getRedTeams())
                .dateTime(request.getDateTime())
                .gameDuration(request.getGameDuration())
                .build();
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


    public Mono<String> getUserResults(String name) {
        String key = String.format(GAME_RESULT_USER_KEY, name);

        return RedisTemplateUserResult.opsForValue().get(key)
                .flatMap(json -> Mono.just(json))
                .switchIfEmpty(Mono.defer(() ->
                        getGameResultsByName(name)
                            .collectList()
                            .flatMap(results -> {
                                if (results.isEmpty()) {
                                    return Mono.just("전적 검색 결과가 없습니다.");
                                } else {
                                    try {
                                        String json = objectMapper.writeValueAsString(results);
                                        return RedisTemplateUserResult.opsForValue().set(key, json)
                                                .then(RedisTemplateUserResult.expire(key, Duration.ofMinutes(expireTime)))
                                                .then(Mono.just("검색된 전적: " + json));
                                    } catch (JsonProcessingException e) {
                                        return Mono.error(new RuntimeException("JSON 직렬화 오류", e));
                                    }
                                }
                            })));
    }



    public Flux<GameResultDocument> getGameResultsByName(String name) {
        return resultRepository.searchByUserName(name);
    }

    public Mono<Void> migrateAllResultsToElasticsearch() {
        return fetchAllResultsFromDatabase()
                .flatMap(result -> fetchClientsForResult(result)
                        .flatMap(clients -> saveResultToElasticsearch(result, clients))
                        .then())
                .then();
    }

    private Flux<GameResult> fetchAllResultsFromDatabase() {
        return resultR2dbcRepository.findAll();
    }

    private Mono<List<Client>> fetchClientsForResult(GameResult result) {
        return clientRepository.findByGameResultId(result.getId()).collectList();
    }

    private Mono<GameResultDocument> saveResultToElasticsearch(GameResult result, List<Client> clients) {
        GameResultDocument document = mapGameResultToDocument(result, clients);
        return resultRepository.save(document);
    }

    private GameResultDocument mapGameResultToDocument(GameResult result, List<Client> clients) {
        List<ClientRequest> blueTeams = clients.stream()
                .filter(client -> "blue".equals(client.getTeam()))
                .map(this::mapToClientRequest)
                .collect(Collectors.toList());

        List<ClientRequest> redTeams = clients.stream()
                .filter(client -> "red".equals(client.getTeam()))
                .map(this::mapToClientRequest)
                .collect(Collectors.toList());

        return GameResultDocument.builder()
                .spaceId(result.getCode())
                .state("success")
                .channel(result.getChannel())
                .room(result.getRoom())
                .winTeam(result.getWinTeam())
                .loseTeam(result.getLoseTeam())
                .blueTeams(blueTeams)
                .redTeams(redTeams)
                .dateTime(result.getDateTime())
                .gameDuration(result.getGameDuration())
                .build();
    }

    private ClientRequest mapToClientRequest(Client client) {
        List<Integer> itemList = convertStringToItems(client.getItemList());
        log.info("TEST : "+itemList.toString());
        return ClientRequest.builder()
                .clientindex(client.getUserId().intValue())
                .socket(client.getSocket())
                .champindex(client.getChamp())
                .user_name(client.getName())
                .team(client.getTeam())
                .channel(client.getChannel())
                .room(client.getRoom())
                .kill(client.getKills())
                .death(client.getDeaths())
                .assist(client.getAssists())
                .gold(client.getGold())
                .level(client.getLevel())
                .maxhp(client.getMaxhp())
                .maxmana(client.getMaxmana())
                .attack(client.getAttack())
                .critical(client.getCritical())
                .criProbability(client.getCriProbability())
                .attrange(client.getAttrange())
                .attspeed(client.getAttspeed())
                .movespeed(client.getMovespeed())
                .itemList(itemList)
                .build();
    }

    private List<Integer> convertStringToItems(String itemList) {
        if (itemList == null || itemList.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            String cleanedItemList = itemList.replaceAll("[\\[\\]]", "");
            return Arrays.stream(cleanedItemList.split(","))
                    .map(String::trim)
                    .map(Integer::valueOf)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            System.err.println("Invalid number format in itemList: " + itemList);
            throw e;
        }
    }



}


