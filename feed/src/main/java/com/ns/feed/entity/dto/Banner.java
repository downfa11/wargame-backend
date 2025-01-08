package com.ns.feed.entity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Banner {
    @JsonProperty("Name")
    private String name;

    @JsonProperty("Url")
    private String url;

    @JsonProperty("ImageUrl")
    private String imageUrl;
}
