package com.popIt.pop_it.domain.space.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class KakaoLocalResDTO {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RegionCode(
            List<Document> documents
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Document(
            @JsonProperty("region_type") String regionType,
            @JsonProperty("region_1depth_name") String region1DepthName,
            @JsonProperty("region_2depth_name") String region2DepthName,
            @JsonProperty("region_3depth_name") String region3DepthName
    ) {}
}
