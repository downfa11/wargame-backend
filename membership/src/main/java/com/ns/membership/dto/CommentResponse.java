package com.ns.membership.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentResponse {
    private Long id;
    private Long userId;
    private String nickname;
    private Long boardId;

    private String content;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
