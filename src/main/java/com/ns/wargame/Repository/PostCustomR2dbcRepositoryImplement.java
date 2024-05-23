package com.ns.wargame.Repository;

import com.ns.wargame.Domain.Post;
import com.ns.wargame.Domain.User;
import com.ns.wargame.Domain.dto.PostSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Repository
@RequiredArgsConstructor
public class PostCustomR2dbcRepositoryImplement implements PostCustomR2dbcRepository{
   private final DatabaseClient databaseClient;

    @Override
    public Flux<Post> findAllByUserId(Long userId) {
        var sql = """
                SELECT p.id, p.user_id, p.category_id, p.title, p.content, p.sort_status, p.created_at, p.updated_at,
                   u.id as uid, u.password, u.name, u.email, u.elo, u.created_at as u_created_at, u.updated_at as u_updated_at
                FROM posts p
                LEFT JOIN users u ON p.user_id = u.id
                WHERE p.user_id = :userId
                """;
        return databaseClient.sql(sql)
                .bind("userId", userId)
                .map(row -> Post.builder()
                        .id(row.get("id", Long.class))
                        .userId(row.get("user_id", Long.class))
                        .categoryId(row.get("category_id", Long.class))
                        .title(row.get("title", String.class))
                        .content(row.get("content", String.class))
                        .sortStatus(row.get("sort_status", Post.SortStatus.class))
                        .createdAt(row.get("created_at", LocalDateTime.class))
                        .updatedAt(row.get("updated_at", LocalDateTime.class))
                        .user(User.builder()
                                .id(row.get("uid", Long.class))
                                .password(row.get("password", String.class))
                                .name(row.get("name", String.class))
                                .email(row.get("email", String.class))
                                .elo(row.get("elo", Long.class))
                                .createdAt(row.get("u_created_at", LocalDateTime.class))
                                .updatedAt(row.get("u_updated_at", LocalDateTime.class))
                                .build())
                        .build())
                .all();
    }


}