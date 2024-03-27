package com.ns.wargame.Domain.dto;

import com.ns.wargame.Domain.Post;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserPostResponse {

    private Long id;
    private String name;
    private String title;
    private String content;
    private LocalDateTime createAt;
    private LocalDateTime updateAt;
    public static UserPostResponse of(Post post){
        return UserPostResponse.builder()
                .id(post.getId())
                .name(post.getUser().getName())
                .title(post.getTitle())
                .content(post.getContent())
                .createAt(post.getCreatedAt())
                .updateAt(post.getUpdatedAt())
                .build();
    }
}
