package com.abhishek.coursesearch.model;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.CompletionField;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.suggest.Completion;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(indexName = "courses")
public class CourseDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Keyword)
    private String type; // ONE_TIME, COURSE, CLUB

    @Field(type = FieldType.Keyword)
    private String gradeRange; // e.g., "1st–3rd"

    @Field(type = FieldType.Integer)
    private int minAge;

    @Field(type = FieldType.Integer)
    private int maxAge;

    @Field(type = FieldType.Double)
    private double price;

    @Field(type = FieldType.Date, format = DateFormat.strict_date_time)
    private Instant nextSessionDate;

    @CompletionField
    private Completion suggest;

    // Optional helper constructor for setting suggest field if needed manually
    public void initializeSuggest() {
        suggest = new Completion(List.of(title));
    }

}
