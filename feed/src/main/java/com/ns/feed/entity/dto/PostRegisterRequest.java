package com.ns.feed.entity.dto;

import com.ns.feed.entity.Post;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PostRegisterRequest {
    private Post.SortStatus sortStatus;
    private Long categoryId;
    private String title;
    private String content;

    private LocalDateTime eventStartDate;
    private LocalDateTime eventEndDate;
}
