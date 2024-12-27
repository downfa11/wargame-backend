package com.ns.membership.entity.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostResponse {
    private Long id;
    private Long userId;
    private Long categoryId;
    private SortStatus sortStatus;
    private String title;
    private String content;

    private List<CommentResponse> commentList;
    private Long comments;
    private Long likes;
    private Long views;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum SortStatus {
        announce, free
    }
}

