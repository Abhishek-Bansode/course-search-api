package com.abhishek.coursesearch.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.DateRangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.NumberRangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch.core.search.CompletionSuggester;
import co.elastic.clients.elasticsearch.core.search.FieldSuggester;
import co.elastic.clients.elasticsearch.core.search.Suggester;
import com.abhishek.coursesearch.model.CourseDocument;
import com.abhishek.coursesearch.model.SearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.suggest.response.Suggest;
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

        // Full-text search with fuzzy match on title, normal on description
        if (q != null && !q.isBlank()) {
            Query fuzzyTitle = MatchQuery.of(m -> m
                    .field("title")
                    .query(q)
                    .fuzziness("AUTO")  // Enable fuzzy match (e.g., 'dinors' ~ 'Dinosaurs')
            )._toQuery();

            Query normalDescription = MatchQuery.of(m -> m
                    .field("description")
                    .query(q)
            )._toQuery();

            mustQueries.add(BoolQuery.of(b -> b
                    .should(fuzzyTitle)
                    .should(normalDescription)
                    .minimumShouldMatch("1")
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
            query.addSort(Sort.by("price").ascending());
        } else if ("priceDesc".equalsIgnoreCase(sort)) {
            query.addSort(Sort.by("price").descending());
        } else {
            query.addSort(Sort.by("nextSessionDate").ascending());
        }

        // Execute the search
        SearchHits<CourseDocument> hits = elasticsearchOperations.search(query, CourseDocument.class);
        List<CourseDocument> results = hits.get().map(SearchHit::getContent).toList();

        return new SearchResponse(hits.getTotalHits(), results);
    }

    public List<String> suggestTitles(String q) {
        // Build the suggester using Completion field
        Suggester suggester = Suggester.of(s -> s
                .text(q) // <-- prefix passed globally
                .suggesters("title-suggest", FieldSuggester.of(fs -> fs
                        .completion(CompletionSuggester.of(cs -> cs
                                .field("suggest")
                                .skipDuplicates(true)
                                .size(10)
                        ))
                ))
        );

        // Create NativeQuery with Suggester
        NativeQuery query = NativeQuery.builder()
                .withSuggester(suggester)
                .build();

        // Perform the suggest query
        SearchHits<CourseDocument> prefixHits = elasticsearchOperations.search(query, CourseDocument.class);

        // Extract and flatten the suggestions
        Suggest suggest = prefixHits.getSuggest();

        List<String> prefixSuggestions = new ArrayList<>();
        if (suggest != null && suggest.getSuggestion("title-suggest") != null) {
            prefixSuggestions = suggest.getSuggestion("title-suggest") // get the Suggestion object by name
                    .getEntries()                         // get its entries
                    .stream()
                    .flatMap(entry -> entry.getOptions().stream()) // flatten all options
                    .map(Suggest.Suggestion.Entry.Option::getText)               // extract the suggestion text
                    .toList();
        }

        if (!prefixSuggestions.isEmpty()) {
            return prefixSuggestions;
        }

        // Step 2: Fallback to fuzzy match if prefix-based suggestions are empty
        Query fuzzyQuery = MatchQuery.of(m -> m
                .field("title")
                .query(q)
                .fuzziness("AUTO")
        )._toQuery();

        NativeQuery fallbackQuery = NativeQuery.builder()
                .withQuery(fuzzyQuery)
                .withPageable(PageRequest.of(0, 10))
                .build();

        SearchHits<CourseDocument> fallbackHits = elasticsearchOperations.search(fallbackQuery, CourseDocument.class);

        return fallbackHits.get()
                .map(SearchHit::getContent)
                .map(CourseDocument::getTitle)
                .toList();
    }

}
