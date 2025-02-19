package com.ns.feed.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
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
