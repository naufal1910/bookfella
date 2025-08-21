package com.bookfella.booking.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.annotations.InnerField;

import java.math.BigDecimal;
import java.util.List;

@Document(indexName = "hotels")
public class HotelDocument {

    @Id
    private String id;

    @MultiField(
            mainField = @Field(name = "name", type = FieldType.Text),
            otherFields = { @InnerField(suffix = "kw", type = FieldType.Keyword) }
    )
    private String name;

    @Field(name = "city", type = FieldType.Keyword)
    private String city;

    @Field(name = "tags", type = FieldType.Keyword)
    private List<String> tags;

    @Field(name = "priceFrom", type = FieldType.Scaled_Float, scalingFactor = 100)
    private BigDecimal priceFrom;

    public HotelDocument() {}

    public HotelDocument(String id, String name, String city, List<String> tags, BigDecimal priceFrom) {
        this.id = id;
        this.name = name;
        this.city = city;
        this.tags = tags;
        this.priceFrom = priceFrom;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public BigDecimal getPriceFrom() { return priceFrom; }
    public void setPriceFrom(BigDecimal priceFrom) { this.priceFrom = priceFrom; }
}
