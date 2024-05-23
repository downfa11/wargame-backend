package com.ns.wargame.Domain.dto;

import com.ns.wargame.Domain.Comment;
import lombok.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    private Long id;
    private Long userId;
    private Long boardId;
    private String nickname;
    private String body;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CommentResponse of(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .userId(comment.getUserId())
                .boardId(comment.getBoardId())
                .body(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
