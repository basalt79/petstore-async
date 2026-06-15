package com.petstore;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.petstore.config.MongoConfig;
import io.javalin.Javalin;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Testcontainers
public abstract class BaseIntegrationTest {

    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

    private MongoConfig mongoConfig;

    protected static final String API_KEY = "test-key";
    protected static final ObjectMapper MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    protected Javalin app;
    protected int port;
    protected HttpClient http;

    @BeforeEach
    void setUp() {
        mongoConfig = new MongoConfig(MONGO.getConnectionString());
        var db = mongoConfig.getDatabase();
        Mono.from(db.getCollection("pets").deleteMany(new Document())).block();
        Mono.from(db.getCollection("orders").deleteMany(new Document())).block();
        Mono.from(db.getCollection("users").deleteMany(new Document())).block();

        app = AppConfig.create(mongoConfig, API_KEY);
        app.start(0);
        port = app.port();
        http = HttpClient.newHttpClient();
    }

    @AfterEach
    void tearDown() {
        app.stop();
        mongoConfig.close();
    }

    protected String url(String path) {
        return "http://localhost:" + port + path;
    }

    protected HttpResponse<String> get(String path) throws Exception {
        return http.send(HttpRequest.newBuilder()
            .uri(URI.create(url(path)))
            .GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    protected HttpResponse<String> getWithKey(String path) throws Exception {
        return http.send(HttpRequest.newBuilder()
            .uri(URI.create(url(path)))
            .header("X-API-KEY", API_KEY)
            .GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    protected HttpResponse<String> post(String path, Object body) throws Exception {
        String json = MAPPER.writeValueAsString(body);
        return http.send(HttpRequest.newBuilder()
            .uri(URI.create(url(path)))
            .header("Content-Type", "application/json")
            .header("X-API-KEY", API_KEY)
            .POST(HttpRequest.BodyPublishers.ofString(json)).build(),
            HttpResponse.BodyHandlers.ofString());
    }

    protected HttpResponse<String> put(String path, Object body) throws Exception {
        String json = MAPPER.writeValueAsString(body);
        return http.send(HttpRequest.newBuilder()
            .uri(URI.create(url(path)))
            .header("Content-Type", "application/json")
            .header("X-API-KEY", API_KEY)
            .PUT(HttpRequest.BodyPublishers.ofString(json)).build(),
            HttpResponse.BodyHandlers.ofString());
    }

    protected HttpResponse<String> delete(String path) throws Exception {
        return http.send(HttpRequest.newBuilder()
            .uri(URI.create(url(path)))
            .header("X-API-KEY", API_KEY)
            .DELETE().build(), HttpResponse.BodyHandlers.ofString());
    }
}
