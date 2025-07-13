package com.abhishek.coursesearch.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SearchResponse {
    private long total;
    private List<CourseDocument> courses;
}
