package com.ns.feed.adapter.out.persistence.image;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("images")
public class Image {
    @Id
    private Long id;

    @Column("post_id")
    private Long postId;

    private String url;
}
