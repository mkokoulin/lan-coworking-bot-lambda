package com.lan.app.flows.news.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** One item from GET {baserowUrl}/coworking/v1/blog (CoworkingNewResponse on lan-baserow-api-lambda). */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CoworkingNewsDto {
    public String titleEn;
    public String titleRu;
    public String bodyEn;
    public String bodyRu;
    public String link;
}
