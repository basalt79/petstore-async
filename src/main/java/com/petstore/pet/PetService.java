package com.petstore.pet;

import com.petstore.pet.model.Pet;
import com.petstore.pet.model.PetStatus;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.NotFoundResponse;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class PetService {

    private final PetRepository repo;

    public PetService(PetRepository repo) {
        this.repo = repo;
    }

    public Mono<Pet> addPet(Pet pet) {
        if (pet.getId() == null) {
            pet.setId(ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE));
        }
        return repo.insert(pet).thenReturn(pet);
    }

    public Mono<Pet> updatePet(Pet pet) {
        return repo.updateById(pet)
                .flatMap(matched -> matched
                        ? Mono.just(pet)
                        : Mono.error(new NotFoundResponse("Pet not found")));
    }

    public Mono<List<Pet>> findByStatus(String status) {
        try {
            PetStatus.fromValue(status);
        } catch (IllegalArgumentException e) {
            return Mono.error(new BadRequestResponse("Invalid status value: " + status));
        }
        return repo.findByStatus(status);
    }

    public Mono<List<Pet>> findByTags(List<String> tags) {
        return repo.findByTags(tags);
    }

    public Mono<Pet> getPetById(long petId) {
        return repo.findById(petId)
                .switchIfEmpty(Mono.error(new NotFoundResponse("Pet not found")));
    }

    public Mono<Void> updatePetByForm(long petId, String name, String status) {
        return repo.updateNameAndStatus(petId, name, status)
                .flatMap(matched -> matched
                        ? Mono.empty()
                        : Mono.error(new NotFoundResponse("Pet not found")));
    }

    public Mono<Void> deletePet(long petId) {
        return repo.deleteById(petId)
                .flatMap(deleted -> deleted
                        ? Mono.empty()
                        : Mono.error(new NotFoundResponse("Pet not found")));
    }

    public Mono<String> uploadImage(long petId, InputStream imageStream, String filename) {
        return Mono.fromCallable(() -> {
            byte[] bytes = imageStream.readAllBytes();
            return Base64.getEncoder().encodeToString(bytes);
        }).flatMap(base64 -> repo.updatePhotoBase64(petId, base64)
                .flatMap(matched -> matched
                        ? Mono.just(filename != null ? filename : "file")
                        : Mono.error(new NotFoundResponse("Pet not found"))));
    }
}
