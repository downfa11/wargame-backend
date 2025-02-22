package com.ns.match.adapter.out;

import static com.ns.common.task.TaskUseCase.createSubTask;
import static com.ns.common.task.TaskUseCase.createTask;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ns.common.anotation.PersistanceAdapter;
import com.ns.common.task.SubTask;
import com.ns.common.task.Task;
import com.ns.match.application.port.out.ProcessMatchQueuePort;
import com.ns.match.application.port.out.task.TaskProducerPort;
import com.ns.match.application.service.MatchResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@PersistanceAdapter
@RequiredArgsConstructor
public class RedisMatchProcessAdapter implements ProcessMatchQueuePort {
    public static final Long MAX_ALLOW_USER_COUNT = 2L;


    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final String MATCH_WAIT_KEY ="users:queue:%s:wait";

    private final TaskProducerPort taskProducerPort;
    private final ObjectMapper mapper;


    @Override
    public Mono<Void> process(String queueKey) {
        return reactiveRedisTemplate.
                scan(ScanOptions.scanOptions()
                        .match(queueKey)
                        .count(3) // 매칭 큐의 종류
                        .build())
                .map(this::extractQueueName)
                .collectList()
                .flatMap(this::processAllQueue);
    }

    private String extractQueueName(String key) {
        return key.split(":")[2];
    }

    @Override
    public Mono<Void> processAllQueue(List<String> queues) {
        return Flux.fromIterable(queues)
                .flatMap(this::processQueue)
                .then();
    }

    @Override
    public Mono<Void> processQueue(String queue) {
        return reactiveRedisTemplate.executeInSession(session ->
                processQueueInRange(queue, 100)).then();
    }

    @Override
    public Mono<Void> processQueueInRange(String queue, int maxProcessCount) {
        AtomicBoolean stopProcessing = new AtomicBoolean(false);

        return Flux.range(0, maxProcessCount)
                .takeWhile(i -> !stopProcessing.get())
                .flatMap(i -> reactiveRedisTemplate.opsForZSet()
                        .popMin(MATCH_WAIT_KEY.formatted(queue), MAX_ALLOW_USER_COUNT)
                        .collectList()
                        .flatMap(members -> {
                            if (members.isEmpty()) {
                                log.info("빈집입니다. {}", queue);
                                stopProcessing.set(true);
                                return Mono.empty();
                            }

                            List<String> memberValues = members.stream()
                                    .map(TypedTuple::getValue)
                                    .collect(Collectors.toList());

                            if (memberValues.size() < MAX_ALLOW_USER_COUNT) {
                                log.info("{} 매칭 큐는 MAX_ALLOW_USER_COUNT를 충족시키지 못하는 찌꺼기 남았음: {}", queue, memberValues.size());
                                return handleMatchError(queue, memberValues)
                                        .doOnTerminate(() -> stopProcessing.set(true));
                            }

                            return handleMatchFound(queue, memberValues)
                                    .onErrorResume(e -> {
                                        log.error("Error handleMatchFound {}: {}", queue, e.getMessage());
                                        return handleMatchError(queue, memberValues);
                                    });
                        }), 1)
                .then();
    }


    public Mono<Void> handleMatchError(String queue, List<String> members) {
        return Flux.fromIterable(members)
                .flatMap(member -> reactiveRedisTemplate.opsForZSet()
                        .add(MATCH_WAIT_KEY.formatted(queue), member, 0).then())
                .then();
    }


    public Mono<Void> handleMatchFound(String queue, List<String> members) {
        String spaceId = UUID.randomUUID().toString();
        MatchResponse matchResponse = MatchResponse.fromMembers(spaceId, members);
        members.forEach(memberId -> saveMatchInfo(memberId, matchResponse));

        List<SubTask> subTasks = createSubTaskListMatchResponse(matchResponse);
        Task task = createTaskMatchResponse(subTasks);

        return taskProducerPort.sendTask("task.match.response", task)
                .then(removeMembersFromQueue(queue, members))
                .doOnError(error -> log.error("Error sendTask: " + error.getMessage()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private Task createTaskMatchResponse(List<SubTask> subTasks){
        return createTask("Match response", null, subTasks);
    }
    private List<SubTask> createSubTaskListMatchResponse(MatchResponse matchResponse){
        List<SubTask> subTasks = new ArrayList<>();
        subTasks.add(createSubTaskMatchResponse(matchResponse));
        return subTasks;
    }

    private SubTask createSubTaskMatchResponse(MatchResponse matchResponse){
        return createSubTask("MatchResponse",
                null,
                SubTask.TaskType.match,
                SubTask.TaskStatus.ready,
                matchResponse);
    }

    private void saveMatchInfo(String memberId, MatchResponse matchResponse) {
        try {
            String membershipId = memberId.split(":")[0];
            String json = mapper.writeValueAsString(matchResponse);
            reactiveRedisTemplate.opsForValue().set("matchInfo:" + membershipId, json).subscribe();
        } catch (JsonProcessingException e) {
            log.error("saveMatchInfo JsonProcessingException: ", e);
        }
    }

    private Mono<Void> removeMembersFromQueue(String queue, List<String> members) {
        return reactiveRedisTemplate.opsForZSet()
                .remove(MATCH_WAIT_KEY.formatted(queue), members.toArray())
                .then();
    }
}
