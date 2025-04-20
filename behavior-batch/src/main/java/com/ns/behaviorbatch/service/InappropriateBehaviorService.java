package com.ns.behaviorbatch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
import com.ns.behaviorbatch.domain.Alert;
import com.ns.behaviorbatch.domain.AlertType;
import com.ns.behaviorbatch.domain.LogDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InappropriateBehaviorService {
    private static final Set<String> BAD_WORDS = Set.of("화이트리스트가", "영..", "아닌건", "나도", "아는데...", "일단", "이걸로", "참아줘..");
    private static final List<Pattern> BAD_PATTERNS = List.of(Pattern.compile("f[\\W_]*u[\\W_]*c[\\W_]*k", Pattern.CASE_INSENSITIVE));


    private final ElasticsearchClient elasticsearchClient;


    // 전체 부적절한 행동을 감지하여 알림(Alert) 반환 (checkFrequentDeaths, checkBadwords, checkBadPatterns)
    public Alert isInappropriate(LogDocument log) {
        if ("DEATH".equals(log.getAction())) {
            if (checkFrequentDeaths(log.getUserId())) {
                return new Alert(log.getUserId(), log.getMessage(), AlertType.FREQUENT_DEATH);
            }
        }

        Alert badWordAlert = checkBadWords(log);
        if (badWordAlert != null) {
            return badWordAlert;
        }

        Alert patternAlert = checkBadPatterns(log);
        if (patternAlert != null) {
            return patternAlert;
        }

        return null;
    }

    // 트롤링 행위 확인 - 5분 안에 3번 이상 죽음
    private boolean checkFrequentDeaths(String userId) {
        try {
            SearchRequest searchRequest = new SearchRequest.Builder()
                    .query(QueryBuilders.bool(q -> q
                            .must(m -> m.term(t -> t.field("userId").value(userId)))
                            .must(m -> m.term(t -> t.field("action").value("DEATH")))
                            .must(m -> m.range(r -> r.field("timestamp").gte(JsonData.fromJson("now-5m"))))
                    )).build();

            SearchResponse<LogDocument> response = elasticsearchClient.search(searchRequest, LogDocument.class);

            List<LogDocument> recentDeaths = response.hits().hits().stream()
                    .map(hit -> hit.source())
                    .collect(Collectors.toList());

            return recentDeaths.size() >= 3;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    // 욕설이 포함된 메시지 확인
    private Alert checkBadWords(LogDocument log) {
        String msg = log.getMessage().toLowerCase();

        for (String badWord : BAD_WORDS) {
            if (msg.contains(badWord)) {
                return new Alert(log.getUserId(), log.getMessage(), AlertType.BAD_WORD);
            }
        }
        return null;
    }


    // 비속어 패턴이 포함된 메시지 확인
    private Alert checkBadPatterns(LogDocument log) {
        String msg = log.getMessage().toLowerCase();

        for (Pattern pattern : BAD_PATTERNS) {
            if (pattern.matcher(msg).find()) {
                return new Alert(log.getUserId(), log.getMessage(), AlertType.BAD_PATTERN);
            }
        }
        return null;
    }
}
