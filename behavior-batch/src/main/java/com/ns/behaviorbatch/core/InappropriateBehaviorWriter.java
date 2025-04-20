package com.ns.behaviorbatch.core;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import com.ns.behaviorbatch.domain.Alert;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class InappropriateBehaviorWriter implements ItemWriter<Alert> {
    private static final String ALERT_INDEX_NAME = "alert_index";

    private final ElasticsearchClient elasticsearchClient;


    @Override
    public void write(Chunk<? extends Alert> chunk) {
        List<Alert> alerts = (List<Alert>) chunk.getItems();

        for (Alert alert : alerts) {
            try {
                IndexRequest<Alert> indexRequest = new IndexRequest.Builder<Alert>()
                        .index(ALERT_INDEX_NAME)
                        .document(alert)
                        .build();

                IndexResponse response = elasticsearchClient.index(indexRequest);
                System.out.println("Indexed response id : " + response.id());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
