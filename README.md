# 📚 Course Search API (UndoSchool Internship Assignment – Part A)

### This Spring Boot application implements a **Course Search API** using **Elasticsearch**, enabling users to search for educational courses with support for full-text search, filters, pagination, and sorting.

---

## 🚀 Features Implemented (Part A)

✅ Full-text search on course `title` and `description`  

✅ Filters:
- Age Range: `minAge`, `maxAge`
- Price Range: `minPrice`, `maxPrice`
- Category & Type: exact match
- Next session date: only upcoming courses (`startDate`) 

✅ Sorting:
- Default: by `nextSessionDate` ascending
- `priceAsc`: price low to high
- `priceDesc`: price high to low  

✅ Pagination: `page` and `size` params supported

---


### 📁 Data Setup (50 Courses)
> The application loads 50 diverse course entries into the courses index at startup. You can find the JSON file under: 
> **`src/main/resources/sample-courses.json`**

### 🧪 Testing & Verification
* Verified all filters and sorting via Postman
* Edge cases like blank queries, large page numbers, invalid sort params handled gracefully
* Testing task (Testcontainers) was optional and not included

### 🛠 Tech Stack
* Java 17
* Spring Boot 3.5.3
* Spring Data Elasticsearch (5.5.x)
* Elasticsearch 8.18.x (Java API Client)
* Lombok

### 🚦 How to Run
*  Step 1: Start Elasticsearch with Docker Compose
    ```dockerfile
    docker-compose up -d
    ```
* Step 2: Ensure Elasticsearch is running at localhost:9200
    ```bash
    curl -X GET "localhost:9200" 
    ```
  > Response must be: 
    ```bash 
    {
      "name" : "some-numeric-id",
      "cluster_name" : "your-cluster-name",
      "cluster_uuid" : "your-clusters-unique-id",
      "version" : {
        "number" : "9.0.3",
        "build_flavor" : "default",
        "build_type" : "docker",
        "build_hash" : "cc7302afc8499e83262ba2ceaa96451681f0609d",
        "build_date" : "2025-06-18T22:09:56.772581489Z",
        "build_snapshot" : false,
        "lucene_version" : "10.1.0",
        "minimum_wire_compatibility_version" : "8.18.0",
        "minimum_index_compatibility_version" : "8.0.0"
      },
      "tagline" : "You Know, for Search"
    }
    ```
*  Step 3: Run Spring Boot Application
    - You can start the app via Maven:
    ```bash
      ./mvnw spring-boot:run
    ```
*  Step 4: Index Course Data:

    #### On application startup:
     - The sample-courses.json file (50 entries) is loaded into Elasticsearch automatically
     - If the index doesn't exist, it is created with the correct mapping (especially for nextSessionDate as date)


* Step 5: Verify the api endpoints with different params:
## 🧪 Sample API Requests

Base URL: `http://localhost:8080/api/v1/search`

###  1. Basic Full-Text Search
```http
GET /api/v1/search?q=science
```
> Search for courses with "science" in title or description.

###  2. Filter by Category and Type
```http
GET /api/v1/search?category=Technology&type=COURSE
```
> Search for Technology-related course-type classes.

###  3. Price Filter + Sorting
```http
GET /api/v1/search?minPrice=300&maxPrice=800&sort=priceAsc
```
> Search for courses priced between ₹300 and ₹800, sorted low to high.

###  4. Age Range + Upcoming Classes Only
```http
GET /api/v1/search?minAge=8&maxAge=12&startDate=2025-08-01T00:00:00Z
```
> Search for courses for 8–12 year olds starting on or after Aug 1, 2025.

###  5. Pagination Example
```http
GET /api/v1/search?q=math&page=1&size=5
```
> Second page of math-related courses, 5 per page.


### 📦 Sample Response Format
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
    },
    {
      "id": "course-041",
      "title": "Origami Creations",
      "description": "This is a course titled 'Origami Creations' designed to engage students in hands-on learning.",
      "category": "Science",
      "type": "CLUB",
      "gradeRange": "2nd–4th",
      "minAge": 9,
      "maxAge": 12,
      "price": 467.66,
      "nextSessionDate": "2025-07-17T17:41:52Z"
    },
    {
      "id": "course-029",
      "title": "World Geography Challenge",
      "description": "This is a course titled 'World Geography Challenge' designed to engage students in hands-on learning.",
      "category": "Language",
      "type": "CLUB",
      "gradeRange": "5th–7th",
      "minAge": 8,
      "maxAge": 11,
      "price": 773.8,
      "nextSessionDate": "2025-07-18T17:41:52Z"
    },
    ...
  ]
}
```
