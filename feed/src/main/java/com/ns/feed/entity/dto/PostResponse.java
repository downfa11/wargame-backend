package com.ns.feed.entity.dto;

import com.ns.feed.entity.Comment;
import com.ns.feed.entity.Post;
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
    private String nickname;
    private Long categoryId;
    private Post.SortStatus sortStatus;
    private String title;
    private String content;

    private List<CommentResponse> commentList;
    private Long comments;
    private Long likes;
    private Long views;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PostResponse of(Post post) {
        return PostResponse.builder()
                .id(post.getId())
                .userId(post.getUserId())
                .nickname(post.getNickname())
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
