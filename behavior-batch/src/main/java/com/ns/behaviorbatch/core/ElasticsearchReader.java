package com.ns.behaviorbatch.core;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.ns.behaviorbatch.domain.LogDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ElasticsearchReader implements ItemReader<LogDocument> {
    private static final String LOG_DOCUMENT_INDEX_NAME = "user-behavior-logs";

    private final ElasticsearchClient elasticsearchClient;
    private Iterator<LogDocument> currentIterator;


    @Override
    public LogDocument read() throws Exception {
        if (currentIterator == null || !currentIterator.hasNext()) {
            fetchNextBatch();
        }
        return currentIterator != null && currentIterator.hasNext() ? currentIterator.next() : null;
    }

    private void fetchNextBatch() throws IOException {
        // 지난 1일(현재 시간 기준) 동안의 로그 fetch
        SearchRequest searchRequest = SearchRequest.of(s -> s
                .index(LOG_DOCUMENT_INDEX_NAME)
                .query(q -> q.range(r -> r
                                .field("timestamp")
                                .gte(JsonData.fromJson("now-1d/d"))
                                .lt(JsonData.fromJson("now/d"))
                        ))
                .size(100));

        SearchResponse<LogDocument> response = elasticsearchClient.search(searchRequest, LogDocument.class);

        List<Hit<LogDocument>> hits = response.hits().hits();
        currentIterator = hits.stream()
                .map(Hit::source)
                .iterator();
    }
}
