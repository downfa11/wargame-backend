package com.ns.result.service;

import com.ns.common.dto.ClientRequest;
import com.ns.common.events.ResultRequestEvent;
import com.ns.common.task.SubTask;
import com.ns.common.task.Task;
import com.ns.common.task.TaskUseCase;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class DodgeService {
    private final ResultService resultService;
    private final TaskUseCase taskUseCase;


    public Mono<Void> dodge(ResultRequestEvent result) {
        List<ClientRequest> allTeams = getAllTeams(result);

        if (allTeams.isEmpty()) {
            log.warn("각 팀이 비어 있습니다!");
            return Mono.empty();
        }

        return Flux.fromIterable(allTeams)
                .flatMap(client -> Mono.just(createDodgeSubTask(client.getMembershipId())))
                .collectList()
                .flatMap(subTasks -> resultService.sendTask("task.membership.response", createDodgeTask(subTasks)));
    }

    private List<ClientRequest> getAllTeams(ResultRequestEvent result){
        List<ClientRequest> allTeams = new ArrayList<>();
        allTeams.addAll(result.getBlueTeams());
        allTeams.addAll(result.getRedTeams());
        return allTeams;
    }

    private SubTask createDodgeSubTask(Long membershipId){
        return taskUseCase.createSubTask("Dodge",
                String.valueOf(membershipId),
                SubTask.TaskType.result,
                SubTask.TaskStatus.ready,
                membershipId);
    }

    private Task createDodgeTask(List<SubTask> subTasks){
        return taskUseCase.createTask(
                "Dodge Request",
                null,
                subTasks);
    }
}
