package com.abhishek.coursesearch.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

        List<Query> mustQueries = buildMustQueries(q);
        List<Query> filterQueries = buildFilterQueries(minAge, maxAge, category, type, minPrice, maxPrice, startDate);

        BoolQuery boolQuery = BoolQuery.of(b -> b.must(mustQueries).filter(filterQueries));

        NativeQuery query = NativeQuery.builder()
                .withQuery(boolQuery._toQuery())
                .withPageable(PageRequest.of(page, size))
                .build();

        applySorting(query, sort);

        SearchHits<CourseDocument> hits = elasticsearchOperations.search(query, CourseDocument.class);
        List<CourseDocument> results = hits.get().map(SearchHit::getContent).toList();

        return new SearchResponse(hits.getTotalHits(), results);
    }

    public List<String> suggestTitles(String q) {
        List<String> prefixSuggestions = runPrefixSuggester(q);
        if (!prefixSuggestions.isEmpty()) {
            return prefixSuggestions;
        }
        return runFuzzyFallback(q);
    }

    private List<Query> buildMustQueries(String q) {
        List<Query> must = new ArrayList<>();
        if (q != null && !q.isBlank()) {
            Query fuzzyTitle = MatchQuery.of(m -> m.field("title").query(q).fuzziness("AUTO"))._toQuery();
            Query normalDescription = MatchQuery.of(m -> m.field("description").query(q))._toQuery();
            must.add(BoolQuery.of(b -> b.should(fuzzyTitle).should(normalDescription).minimumShouldMatch("1"))._toQuery());
        }
        return must;
    }

    private List<Query> buildFilterQueries(Integer minAge, Integer maxAge,
                                           String category, String type,
                                           Double minPrice, Double maxPrice,
                                           Instant startDate) {
        List<Query> filters = new ArrayList<>();

        if (category != null && !category.isBlank()) {
            filters.add(MatchQuery.of(m -> m.field("category").query(category))._toQuery());
        }

        if (type != null && !type.isBlank()) {
            filters.add(MatchQuery.of(m -> m.field("type").query(type))._toQuery());
        }

        if (minAge != null || maxAge != null) {
            NumberRangeQuery ageRange = new NumberRangeQuery.Builder()
                    .field("minAge")
                    .gte(minAge != null ? minAge.doubleValue() : null)
                    .lte(maxAge != null ? maxAge.doubleValue() : null)
                    .build();

            filters.add(new RangeQuery.Builder().number(ageRange).build()._toQuery());
        }

        if (minPrice != null || maxPrice != null) {
            NumberRangeQuery priceRange = new NumberRangeQuery.Builder()
                    .field("price")
                    .gte(minPrice)
                    .lte(maxPrice)
                    .build();

            filters.add(new RangeQuery.Builder().number(priceRange).build()._toQuery());
        }

        if (startDate != null) {
            DateRangeQuery dateRange = new DateRangeQuery.Builder()
                    .field("nextSessionDate")
                    .gte(startDate.toString())
                    .build();

            filters.add(new RangeQuery.Builder().date(dateRange).build()._toQuery());
        }

        return filters;
    }

    private void applySorting(NativeQuery query, String sort) {
        if ("priceAsc".equalsIgnoreCase(sort)) {
            query.addSort(Sort.by("price").ascending());
        } else if ("priceDesc".equalsIgnoreCase(sort)) {
            query.addSort(Sort.by("price").descending());
        } else {
            query.addSort(Sort.by("nextSessionDate").ascending());
        }
    }

    private List<String> runPrefixSuggester(String q) {
        Suggester suggester = Suggester.of(s -> s
                .text(q)
                .suggesters("title-suggest", FieldSuggester.of(fs -> fs
                        .completion(CompletionSuggester.of(cs -> cs
                                .field("suggest")
                                .skipDuplicates(true)
                                .size(10)))))
        );

        NativeQuery query = NativeQuery.builder().withSuggester(suggester).build();
        SearchHits<CourseDocument> hits = elasticsearchOperations.search(query, CourseDocument.class);

        Suggest suggest = hits.getSuggest();
        if (suggest != null && suggest.getSuggestion("title-suggest") != null) {
            return suggest.getSuggestion("title-suggest")
                    .getEntries()
                    .stream()
                    .flatMap(entry -> entry.getOptions().stream())
                    .map(Suggest.Suggestion.Entry.Option::getText)
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    private List<String> runFuzzyFallback(String q) {
        Query fuzzyQuery = MatchQuery.of(m -> m.field("title").query(q).fuzziness("AUTO"))._toQuery();
        NativeQuery query = NativeQuery.builder()
                .withQuery(fuzzyQuery)
                .withPageable(PageRequest.of(0, 10))
                .build();

        SearchHits<CourseDocument> hits = elasticsearchOperations.search(query, CourseDocument.class);
        return hits.get().map(SearchHit::getContent).map(CourseDocument::getTitle).toList();
    }
}
