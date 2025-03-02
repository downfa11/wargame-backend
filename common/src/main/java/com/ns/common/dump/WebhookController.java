package com.ns.common.dump;

import com.ns.common.MessageEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/webhook")
public class WebhookController {
    private final String TRIGGERED_HEAP_DUMP_MESSAGE = "Heap dump triggered";
    private final String TRIGGERED_THREAD_DUMP_MESSAGE = "Thread dump triggered";
    private final String NOT_FOUND_ACTION_ERROR_MESSAGE = "Not found this actions for Webhook";

    private final DumpService dumpService;

    @PostMapping
    public Mono<MessageEntity> receiveWebhook(@RequestBody String payload) {
        log.info("[test] - Received alert log: " + payload);

        if (payload.contains("\"alertname\":\"HighHeapUsage\"")) {
            return Mono.just(new MessageEntity(TRIGGERED_HEAP_DUMP_MESSAGE, dumpService.generateHeapDump()));
        }

        else if (payload.contains("\"alertname\":\"ThreadBlockingDetected\"")) {
            return Mono.just(new MessageEntity(TRIGGERED_THREAD_DUMP_MESSAGE, dumpService.generateThreadDump()));
        }

        return Mono.just(new MessageEntity(NOT_FOUND_ACTION_ERROR_MESSAGE,null));
    }
}
