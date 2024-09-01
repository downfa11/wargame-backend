package com.ns.feed.entity.dto;

import com.ns.wargame.Utils.Numberic;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommentRegisterRequest {
    private Long boardId;

    @Numberic
    private String body;

}

