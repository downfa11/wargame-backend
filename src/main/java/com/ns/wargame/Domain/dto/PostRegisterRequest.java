package com.ns.wargame.Domain.dto;

import com.ns.wargame.Domain.Post;
import lombok.Data;

@Data
public class PostRegisterRequest {
    private Post.SortStatus sortStatus;
    private Long categoryId;
    private Long userId;
    private String title;
    private String content;
}
