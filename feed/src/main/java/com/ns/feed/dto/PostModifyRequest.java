package com.ns.feed.dto;

import com.ns.feed.adapter.out.persistence.Post;
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
