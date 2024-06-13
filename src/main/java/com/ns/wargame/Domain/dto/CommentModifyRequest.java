package com.ns.wargame.Domain.dto;

import com.ns.wargame.Utils.Numberic;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommentModifyRequest {
    private Long commentId;

    @Numberic
    private String body;

}

