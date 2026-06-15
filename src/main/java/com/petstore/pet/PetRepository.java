package com.petstore.pet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Updates;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import com.petstore.pet.model.Pet;
import org.bson.Document;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

public class PetRepository {

    private static final JsonWriterSettings RELAXED = JsonWriterSettings.builder()
            .outputMode(JsonMode.RELAXED).build();

    private final MongoCollection<Document> collection;
    private final ObjectMapper mapper;

    public PetRepository(MongoDatabase db, ObjectMapper mapper) {
        this.collection = db.getCollection("pets");
        this.mapper = mapper;
    }

    public Mono<Void> insert(Pet pet) {
        return Mono.fromCallable(() -> Document.parse(mapper.writeValueAsString(pet)))
                .flatMap(doc -> Mono.from(collection.insertOne(doc)))
                .then();
    }

    public Mono<Pet> findById(long petId) {
        return Mono.from(collection.find(Filters.eq("id", petId)).first())
                .map(this::toEntity);
    }

    public Mono<List<Pet>> findByStatus(String status) {
        return Flux.from(collection.find(Filters.eq("status", status)))
                .map(this::toEntity)
                .collectList();
    }

    public Mono<List<Pet>> findByTags(List<String> tagNames) {
        return Flux.from(collection.find(Filters.in("tags.name", tagNames)))
                .map(this::toEntity)
                .collectList();
    }

    public Mono<Boolean> updateById(Pet pet) {
        return Mono.fromCallable(() -> {
            var json = mapper.writeValueAsString(pet);
            var update = Document.parse(json);
            update.remove("_id");
            return update;
        }).flatMap(update -> {
            if (update.containsKey("photoBase64")) {
                return Mono.from(collection.replaceOne(Filters.eq("id", pet.getId()), update))
                        .map(r -> r.getMatchedCount() > 0);
            }
            // photoBase64 is managed exclusively by the upload endpoint — preserve it on PUT
            return Mono.from(collection.find(Filters.eq("id", pet.getId()))
                            .projection(Projections.include("photoBase64")).first())
                    .defaultIfEmpty(new Document())
                    .flatMap(existing -> {
                        if (existing.containsKey("photoBase64")) {
                            update.put("photoBase64", existing.get("photoBase64"));
                        }
                        return Mono.from(collection.replaceOne(Filters.eq("id", pet.getId()), update))
                                .map(r -> r.getMatchedCount() > 0);
                    });
        });
    }

    public Mono<Boolean> updateNameAndStatus(long petId, String name, String status) {
        var updates = new ArrayList<org.bson.conversions.Bson>();
        if (name != null) updates.add(Updates.set("name", name));
        if (status != null) updates.add(Updates.set("status", status));
        if (updates.isEmpty()) return Mono.just(true);
        return Mono.from(collection.updateOne(Filters.eq("id", petId), Updates.combine(updates)))
                .map(r -> r.getMatchedCount() > 0);
    }

    public Mono<Boolean> updatePhotoBase64(long petId, String base64) {
        return Mono.from(collection.updateOne(
                        Filters.eq("id", petId), Updates.set("photoBase64", base64)))
                .map(r -> r.getMatchedCount() > 0);
    }

    public Mono<Boolean> deleteById(long petId) {
        return Mono.from(collection.deleteOne(Filters.eq("id", petId)))
                .map(r -> r.getDeletedCount() > 0);
    }

    private Pet toEntity(Document doc) {
        try {
            doc.remove("_id");
            return mapper.readValue(doc.toJson(RELAXED), Pet.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
