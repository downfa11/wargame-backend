package com.ns.feed.entity.dto;

import com.ns.feed.entity.Post;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class PostModifyRequest {
    private Post.SortStatus sortStatus;
    private Long categoryId;
    private Long boardId;
    private String title;
    private String content;

    private LocalDateTime eventStartDate;
    private LocalDateTime eventEndDate;
}
