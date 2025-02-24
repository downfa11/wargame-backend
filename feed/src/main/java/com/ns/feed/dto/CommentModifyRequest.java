package com.ns.feed.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CommentModifyRequest {
    private Long commentId;
    private String body;

}

