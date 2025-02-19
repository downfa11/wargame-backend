package com.ns.feed.dto;

import com.ns.feed.adapter.out.persistence.post.Post;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostRegisterRequest {
    private Post.SortStatus sortStatus;
    private Long categoryId;
    private String title;
    private String content;

    private LocalDateTime eventStartDate;
    private LocalDateTime eventEndDate;
}
