package com.ns.wargame.Domain.dto;

import com.ns.wargame.Domain.Post;
import lombok.Data;

@Data
public class PostModifyRequest {
    private Post.SortStatus sortStatus;
    private Long categoryId;
    private Long boardId;
    private String title;
    private String content;
}
