package com.ns.feed.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
public class CommentRegisterRequest {
    private Long boardId;
    private String body;

}

