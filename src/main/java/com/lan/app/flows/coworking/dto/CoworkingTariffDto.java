package com.lan.app.flows.coworking.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** One item from GET {baserowUrl}/coworking/v1/tariffs (CoworkingTariffResponse on lan-baserow-api-lambda). */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CoworkingTariffDto {
    public String name;
    public Integer price;
    public Integer discount;
    public String discountDescriptionRu;
    public String discountDescriptionEn;
}
