package com.ns.feed.entity.dto;

import com.ns.feed.entity.Post;
import com.ns.wargame.Domain.Post;
import lombok.Data;

@Data
public class PostRegisterRequest {
    private Post.SortStatus sortStatus;
    private Long categoryId;
    private String title;
    private String content;
}
