package com.abhishek.coursesearch.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.abhishek.coursesearch.model.CourseDocument;
import com.abhishek.coursesearch.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@RequiredArgsConstructor
@Component
@Slf4j
public class CourseDataLoader implements ApplicationRunner {

    private final CourseRepository courseRepository;
    private final ObjectMapper objectMapper; // this is auto-configured by Spring

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Loading sample-courses.json...");

        try (InputStream is = getClass().getClassLoader().getResourceAsStream("sample-courses.json")) {
            if (is == null) {
                log.error("sample-courses.json not found in resources.");
                return;
            }

            List<CourseDocument> courses = objectMapper.readValue(is, new TypeReference<>() {});
            courseRepository.saveAll(courses);
            log.info("Successfully indexed {} courses into Elasticsearch.", courses.size());
        } catch (Exception e) {
            log.error("Failed to load and index courses", e);
        }
    }
}

