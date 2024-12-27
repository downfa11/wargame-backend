package com.ns.feed.repository;


import com.ns.feed.entity.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class PostCustomR2dbcRepositoryImplement implements PostCustomR2dbcRepository{
   private final DatabaseClient databaseClient;

    @Override
    public Flux<Post> findAllByUserId(Long userId) {
        var sql = """
                SELECT p.id, p.user_id, p.nickname, p.category_id, p.title, p.content, p.sort_status, p.created_at, p.updated_at
                FROM posts p
                WHERE p.user_id = :userId
                """;
        return databaseClient.sql(sql)
                .bind("userId", userId)
                .map(row -> Post.builder()
                        .id(row.get("id", Long.class))
                        .userId(row.get("user_id", Long.class))
                        .nickname(row.get("nickname",String.class))
                        .categoryId(row.get("category_id", Long.class))
                        .title(row.get("title", String.class))
                        .content(row.get("content", String.class))
                        .sortStatus(row.get("sort_status", Post.SortStatus.class))
                        .createdAt(row.get("created_at", LocalDateTime.class))
                        .updatedAt(row.get("updated_at", LocalDateTime.class))
                        .build())
                .all();
    }


}