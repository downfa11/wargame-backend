package com.ns.membership.application.port.out;

import com.ns.common.task.Task;
import com.ns.membership.dto.PostSummary;
import java.util.List;
import reactor.core.publisher.Mono;

public interface TaskProducerPort {
    Mono<Void> sendTask(String topic, Task task);
    Mono<List<PostSummary>> getUserPosts(Long membershipId);
}

