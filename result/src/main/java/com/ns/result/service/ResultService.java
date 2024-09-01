package com.ns.result.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ns.result.domain.ClientRequest;
import com.ns.result.domain.ResultRequest;
import com.ns.result.domain.entity.Result;
import com.ns.result.repository.ResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.SenderResult;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResultService implements ApplicationRunner {


    private final ResultRepository resultRepository;

    private final ReactiveKafkaConsumerTemplate<String, String> ResultConsumerTemplate;
    private final ReactiveKafkaProducerTemplate<String, String> MembershipProducerTemplate;


    public Mono<SenderResult<Void>> MembershipSendMessage(String topic, String key, String message){
        return MembershipProducerTemplate.send(topic, key, message);
    }

    private String convertItemsToString(List<Integer> itemList){
        String itemListAsString = itemList.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",", "[", "]"));
        return itemListAsString;
    }

    private Mono<Void> updateEloAndSaveUsers(String winTeam, List<ResultUpdate> updates) {

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

    private long calculateOpposingTeamElo(String curTeam, List<ResultUpdate> updates) {
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


    public Mono<Result> saveResult(ResultRequest request) {
        Result document = mapToResultDocument(request);
        return resultRepository.save(document);
    }

    private Result mapToResultDocument(ResultRequest request) {
        return Result.builder()
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

    public Mono<Void> dodge(ResultRequest result) {

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


    public Flux<Result> getGameResultsByName(String name) {
        return resultRepository.searchByUserName(name);
    }

    @Override
    public void run(ApplicationArguments args){

        this.ResultConsumerTemplate
                .receiveAutoAck()
                .doOnNext(r -> {
                    log.info("received message : "+r.value());

                    ObjectMapper mapper = new ObjectMapper();
                    ResultRequest result;
                    try {
                        result = mapper.readValue(r.value(), ResultRequest.class);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }

                    String state = result.getState();

                    if(state.equals("success")){
                        saveResult(result).subscribe();}

                    else if(state.equals("dodge")){
                        dodge(result).subscribe();}

                })
                .doOnError(e -> {System.out.println("Error receiving: " + e);})
                .subscribe();
    }
}


