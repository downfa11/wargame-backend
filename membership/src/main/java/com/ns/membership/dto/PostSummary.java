package com.ns.membership.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostSummary {
    private Long id;
    private SortStatus sortStatus;
    private String nickname;
    private String title;
    private Long likes;
    private Long comments;
    private Long views;
    private LocalDateTime createdAt;

    public enum SortStatus{
        ANNOUNCE, EVENT, FREE
    }
}
