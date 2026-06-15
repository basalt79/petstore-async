package com.petstore.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import com.petstore.store.model.Order;
import org.bson.Document;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Map;

public class StoreRepository {

    private static final JsonWriterSettings RELAXED = JsonWriterSettings.builder()
            .outputMode(JsonMode.RELAXED).build();

    private final MongoCollection<Document> collection;
    private final ObjectMapper mapper;

    public StoreRepository(MongoDatabase db, ObjectMapper mapper) {
        this.collection = db.getCollection("orders");
        this.mapper = mapper;
    }

    public Mono<Void> insert(Order order) {
        return Mono.fromCallable(() -> Document.parse(mapper.writeValueAsString(order)))
                .flatMap(doc -> Mono.from(collection.insertOne(doc)))
                .then();
    }

    public Mono<Order> findById(long orderId) {
        return Mono.from(collection.find(Filters.eq("id", orderId)).first())
                .map(this::toEntity);
    }

    public Mono<Boolean> deleteById(long orderId) {
        return Mono.from(collection.deleteOne(Filters.eq("id", orderId)))
                .map(r -> r.getDeletedCount() > 0);
    }

    public Mono<Map<String, Integer>> getInventoryCounts() {
        return Flux.from(collection.aggregate(Arrays.asList(
                        Aggregates.group("$status", Accumulators.sum("count", 1))
                )))
                .collectMap(doc -> doc.getString("_id"), doc -> doc.getInteger("count"));
    }

    private Order toEntity(Document doc) {
        try {
            doc.remove("_id");
            return mapper.readValue(doc.toJson(RELAXED), Order.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
