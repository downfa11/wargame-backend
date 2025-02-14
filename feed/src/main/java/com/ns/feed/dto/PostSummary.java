package com.ns.feed.dto;

import com.ns.feed.adapter.out.persistence.post.Post;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostSummary {
    private Long id;
    private Post.SortStatus sortStatus;
    private String nickname;
    private String title;
    private Long likes;
    private Long comments;
    private Long views;
    private LocalDateTime createdAt;

    public static PostSummary of(Post post) {
        return PostSummary.builder()
                .id(post.getId())
                .sortStatus(post.getSortStatus())
                .nickname(post.getNickname())
                .title(post.getTitle())
                .comments(post.getComments())
                .createdAt(post.getCreatedAt())
                .build();
    }
}
