package com.ns.feed.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
public class CommentModifyRequest {
    private Long commentId;
    private String body;

}

