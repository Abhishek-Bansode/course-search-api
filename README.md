# 📚 Course Search API (UndoSchool Internship Assignment – Part A)

This Spring Boot application implements a **Course Search API** using **Elasticsearch**, enabling users to search for educational courses with support for full-text search, filters, pagination, and sorting.

---

## ✨ Features Implemented (Part A)

* ✅ Full-text search on course `title` and `description`
* ✅ Filters:

  * Age Range: `minAge`, `maxAge`
  * Price Range: `minPrice`, `maxPrice`
  * Category & Type: exact match
  * Next session date: only upcoming courses (`startDate`)
* ✅ Sorting:

  * Default: by `nextSessionDate` ascending
  * `priceAsc`: price low to high
  * `priceDesc`: price high to low
* ✅ Pagination: `page` and `size` parameters supported

---

## 🚪 How to Run the Application

### ⚖️ Prerequisites

* Java 17+
* Maven
* Docker & Docker Compose

### 🚣 Step 1: Start Elasticsearch via Docker Compose

Create a `docker-compose.yml` file at your project root:

```yaml
version: "3.9"
services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:9.0.3
    container_name: es
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
    ports:
      - "9200:9200"
```

Then run:

```bash
docker-compose up -d
```

Wait for Elasticsearch to be fully up at: [http://localhost:9200](http://localhost:9200)

### 🛠️ Step 2: Run Spring Boot Application

Using Maven:

```bash
./mvnw spring-boot:run
```

Or package and run the JAR:

```bash
./mvnw clean package
java -jar target/course-search-0.0.1-SNAPSHOT.jar
```

### 🔄 Step 3: Data Setup (Automatic on Startup)

On application startup:

* The `courses` index is created (if it doesn't exist)
* Sample data (50 entries) from `sample-courses.json` is indexed automatically

### ⚙️ Jackson Configuration

To support Java 8+ time types (like `Instant`), a custom `ObjectMapper` is configured:

Required dependency:

```xml
<dependency>
  <groupId>com.fasterxml.jackson.datatype</groupId>
  <artifactId>jackson-datatype-jsr310</artifactId>
</dependency>
```

### ⚙️ Elasticsearch Mapping (Date Field)

The `nextSessionDate` field is mapped as:

```json
{
  "type": "date",
  "format": "strict_date_optional_time||epoch_millis"
}
```

This allows date formats like:

* `2025-08-01T00:00:00Z`
* `2025-08-01`
* epoch millis timestamps

---

## 🤞 Sample API Requests

Base URL: `http://localhost:8080/api/v1/search`

### 1. Full-text Search

```http
GET /api/v1/search?q=science
```

### 2. Filter by Category & Type

```http
GET /api/v1/search?category=Technology&type=COURSE
```

### 3. Price Filter + Sorting

```http
GET /api/v1/search?minPrice=300&maxPrice=800&sort=priceAsc
```

### 4. Age Range + Upcoming Courses

```http
GET /api/v1/search?minAge=8&maxAge=12&startDate=2025-08-01T00:00:00Z
```

### 5. Pagination Example

```http
GET /api/v1/search?q=math&page=1&size=5
```

---

## 💾 Sample Response Format

```json
{
  "total": 50,
  "courses": [
    {
      "id": "course-001",
      "title": "Introduction to Astronomy",
      "description": "...",
      "category": "Technology",
      "type": "CLUB",
      "gradeRange": "1st–3rd",
      "minAge": 6,
      "maxAge": 12,
      "price": 256.61,
      "nextSessionDate": "2025-07-24T17:41:52Z"
    }
  ]
}
```

---

## 📊 Tech Stack

* Java 17
* Spring Boot 3.5.3
* Spring Data Elasticsearch 5.5.1
* Elasticsearch 8.18.1
* Lombok

---

## 📅 Testing & Verification

* Verified all endpoints using Postman
* Edge cases (blank query, page out of range, invalid sort param) tested
* Optional integration tests (Testcontainers) not included in Part A
