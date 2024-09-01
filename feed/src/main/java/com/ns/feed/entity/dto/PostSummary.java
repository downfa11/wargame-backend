package com.ns.feed.entity.dto;

import com.ns.feed.entity.Post;
import com.ns.wargame.Domain.Post;
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

    public static PostSummary of(Post post, String nickname) {
        return PostSummary.builder()
                .id(post.getId())
                .sortStatus(post.getSortStatus())
                .nickname(nickname)
                .title(post.getTitle())
                .comments(post.getComments())
                .createdAt(post.getCreatedAt())
                .build();
    }
}
