package com.petstore.config;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import reactor.core.publisher.Mono;

public class MongoConfig {

    private final MongoClient client;
    private final MongoDatabase database;

    public MongoConfig(String uri) {
        this.client = MongoClients.create(uri);
        this.database = client.getDatabase("petstore");
        createIndexes();
    }

    private void createIndexes() {
        Mono.from(database.getCollection("pets").createIndex(
                Indexes.ascending("id"), new IndexOptions().unique(true))).block();
        Mono.from(database.getCollection("pets").createIndex(Indexes.ascending("status"))).block();
        Mono.from(database.getCollection("pets").createIndex(Indexes.ascending("tags.name"))).block();

        Mono.from(database.getCollection("orders").createIndex(
                Indexes.ascending("id"), new IndexOptions().unique(true))).block();

        Mono.from(database.getCollection("users").createIndex(
                Indexes.ascending("username"), new IndexOptions().unique(true))).block();
        Mono.from(database.getCollection("users").createIndex(Indexes.ascending("id"))).block();
    }

    public MongoDatabase getDatabase() {
        return database;
    }

    public void close() {
        client.close();
    }
}
