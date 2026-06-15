package com.petstore.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.model.Filters;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import com.petstore.user.model.User;
import org.bson.Document;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import reactor.core.publisher.Mono;

public class UserRepository {

    private static final JsonWriterSettings RELAXED = JsonWriterSettings.builder()
            .outputMode(JsonMode.RELAXED).build();

    private final MongoCollection<Document> collection;
    private final ObjectMapper mapper;

    public UserRepository(MongoDatabase db, ObjectMapper mapper) {
        this.collection = db.getCollection("users");
        this.mapper = mapper;
    }

    public Mono<Void> insert(User user) {
        return Mono.fromCallable(() -> Document.parse(mapper.writeValueAsString(user)))
                .flatMap(doc -> Mono.from(collection.insertOne(doc)))
                .then();
    }

    public Mono<User> findByUsername(String username) {
        return Mono.from(collection.find(Filters.eq("username", username)).first())
                .map(this::toEntity);
    }

    public Mono<Boolean> updateByUsername(String username, User user) {
        return Mono.fromCallable(() -> {
            Document update = Document.parse(mapper.writeValueAsString(user));
            update.remove("_id");
            return update;
        }).flatMap(update -> Mono.from(collection.replaceOne(Filters.eq("username", username), update))
                .map(r -> r.getMatchedCount() > 0));
    }

    public Mono<Boolean> deleteByUsername(String username) {
        return Mono.from(collection.deleteOne(Filters.eq("username", username)))
                .map(r -> r.getDeletedCount() > 0);
    }

    private User toEntity(Document doc) {
        try {
            doc.remove("_id");
            return mapper.readValue(doc.toJson(RELAXED), User.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
