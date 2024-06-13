package com.ns.wargame.Domain.dto;

import com.ns.wargame.Domain.Comment;
import com.ns.wargame.Domain.Post;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Transient;

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
    private Post.SortStatus sortStatus;
    private String title;
    private String content;

    private List<Comment> commentList;
    private Long comments;
    private Long likes;
    private Long views;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PostResponse of(Post post) {
        return PostResponse.builder()
                .id(post.getId())
                .userId(post.getUserId())
                .sortStatus(post.getSortStatus())
                .categoryId(post.getCategoryId())
                .title(post.getTitle())
                .content(post.getContent())
                .comments(post.getComments())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}
