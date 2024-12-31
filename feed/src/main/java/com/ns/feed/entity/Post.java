package com.ns.feed.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("posts")
public class Post {
    @Id
    private Long id;
    @Column("user_id")
    private Long userId; // FK

    private String nickname;

    @Column("category_id")
    private Long categoryId; // FK

    private String title;
    private String content;

    private Long comments;

    public enum SortStatus{
        ANNOUNCE, EVENT, FREE
    }
    private SortStatus sortStatus;

    private LocalDateTime eventStartDate;
    private LocalDateTime eventEndDate;

    @Column("created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    @Column("updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
