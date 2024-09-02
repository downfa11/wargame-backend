package com.ns.feed.entity.dto;

import com.ns.feed.entity.Post;
import lombok.Data;

@Data
public class PostModifyRequest {
    private Post.SortStatus sortStatus;
    private Long categoryId;
    private Long boardId;
    private String title;
    private String content;
}
