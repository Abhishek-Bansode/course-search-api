package com.abhishek.coursesearch.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.DateRangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.NumberRangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import com.abhishek.coursesearch.model.CourseDocument;
import com.abhishek.coursesearch.model.SearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    public SearchResponse searchCourses(String q, Integer minAge, Integer maxAge,
                                        String category, String type,
                                        Double minPrice, Double maxPrice,
                                        Instant startDate,
                                        String sort, int page, int size) {

        List<Query> mustQueries = new ArrayList<>();
        List<Query> filterQueries = new ArrayList<>();

        // Full-text search on title and description
        if (q != null && !q.isBlank()) {
            mustQueries.add(MultiMatchQuery.of(m -> m
                .fields("title", "description")
                .query(q)
            )._toQuery());
        }

        // Exact matches using keyword fields
        if (category != null && !category.isBlank()) {
            filterQueries.add(MatchQuery.of(m -> m
                    .field("category")
                    .query(category)
            )._toQuery());
        }

        if (type != null && !type.isBlank()) {
            filterQueries.add(MatchQuery.of(m -> m
                    .field("type")
                    .query(type)
            )._toQuery());
        }

        // Age Range
        if (minAge != null || maxAge != null) {
            NumberRangeQuery ageRange = new NumberRangeQuery.Builder()
                .field("minAge")
                .gte(minAge != null ? minAge.doubleValue() : null)
                .lte(maxAge != null ? maxAge.doubleValue() : null)
                .build();

            filterQueries.add(new RangeQuery.Builder().number(ageRange).build()._toQuery());
        }

        // Price Range
        if (minPrice != null || maxPrice != null) {
            NumberRangeQuery priceRange = new NumberRangeQuery.Builder()
                .field("price")
                .gte(minPrice)
                .lte(maxPrice)
                .build();

            filterQueries.add(new RangeQuery.Builder().number(priceRange).build()._toQuery());
        }

        // Start Date Filter
        if (startDate != null) {
            DateRangeQuery dateRange = new DateRangeQuery.Builder()
                .field("nextSessionDate")
                .gte(startDate.toString())
                .build();

            filterQueries.add(new RangeQuery.Builder().date(dateRange).build()._toQuery());
        }

        // Build the BoolQuery using must + filter
        BoolQuery boolQuery = BoolQuery.of(b -> b
            .must(mustQueries)
            .filter(filterQueries)
        );

        // Create the NativeQuery
        NativeQuery query = NativeQuery.builder()
            .withQuery(boolQuery._toQuery())
            .withPageable(PageRequest.of(page, size))
            .build();

        // Sorting
        if ("priceAsc".equalsIgnoreCase(sort)) {
            query.addSort(org.springframework.data.domain.Sort.by("price").ascending());
        } else if ("priceDesc".equalsIgnoreCase(sort)) {
            query.addSort(org.springframework.data.domain.Sort.by("price").descending());
        } else {
            query.addSort(org.springframework.data.domain.Sort.by("nextSessionDate").ascending());
        }

        // Execute the search
        SearchHits<CourseDocument> hits = elasticsearchOperations.search(query, CourseDocument.class);
        List<CourseDocument> results = hits.get().map(SearchHit::getContent).toList();

        return new SearchResponse(hits.getTotalHits(), results);
    }
}
