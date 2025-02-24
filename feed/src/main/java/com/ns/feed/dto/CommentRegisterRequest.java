package com.ns.feed.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CommentRegisterRequest {
    private Long boardId;
    private String body;

}

