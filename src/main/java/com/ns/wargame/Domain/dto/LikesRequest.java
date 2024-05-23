package com.ns.wargame.Domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LikesRequest {

    private Long userId;
    private Boolean addLike;
}
