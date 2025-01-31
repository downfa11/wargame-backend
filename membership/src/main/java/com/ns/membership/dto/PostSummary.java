package com.ns.membership.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostSummary {
    private Long id;
    private PostResponse.SortStatus sortStatus;
    private String nickname;
    private String title;
    private Long likes;
    private Long comments;
    private Long views;
    private LocalDateTime createdAt;
}
