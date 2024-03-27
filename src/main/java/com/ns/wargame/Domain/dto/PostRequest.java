package com.ns.wargame.Domain.dto;

import lombok.Data;

@Data
public class PostRequest {
    private Long userId;
    private String title;
    private String content;
}
