package com.ns.membership.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
public class CommentResponse {
    private Long id;
    private Long userId;
    private String nickname;
    private Long boardId;

    private String content;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
