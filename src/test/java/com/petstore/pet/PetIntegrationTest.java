package com.petstore.pet;

import com.fasterxml.jackson.core.type.TypeReference;
import com.petstore.BaseIntegrationTest;
import com.petstore.pet.model.Category;
import com.petstore.pet.model.Pet;
import com.petstore.pet.model.PetStatus;
import com.petstore.pet.model.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PetIntegrationTest extends BaseIntegrationTest {

    private Pet buildPet(long id) {
        Pet pet = new Pet();
        pet.setId(id);
        pet.setName("Fido");
        pet.setCategory(new Category(1L, "dogs"));
        pet.setTags(List.of(new Tag(1L, "friendly")));
        pet.setStatus(PetStatus.AVAILABLE);
        return pet;
    }

    @Test
    void addAndGetPet() throws Exception {
        var addResp = post("/api/v3/pet", buildPet(1L));
        assertEquals(200, addResp.statusCode());

        var getResp = get("/api/v3/pet/1");
        assertEquals(200, getResp.statusCode());
        Pet retrieved = MAPPER.readValue(getResp.body(), Pet.class);
        assertEquals("Fido", retrieved.getName());
        assertEquals("available", retrieved.getStatus().getValue());
    }

    @Test
    void getPetNotFound() throws Exception {
        var resp = get("/api/v3/pet/999");
        assertEquals(404, resp.statusCode());
    }

    @Test
    void findByStatus() throws Exception {
        post("/api/v3/pet", buildPet(10L));
        Pet pending = buildPet(11L);
        pending.setStatus(PetStatus.PENDING);
        post("/api/v3/pet", pending);

        var resp = get("/api/v3/pet/findByStatus?status=available");
        assertEquals(200, resp.statusCode());
        List<Pet> pets = MAPPER.readValue(resp.body(), new TypeReference<>() {});
        assertEquals(1, pets.size());
        assertEquals("available", pets.get(0).getStatus().getValue());
    }

    @Test
    void findByTags() throws Exception {
        post("/api/v3/pet", buildPet(20L));
        var resp = get("/api/v3/pet/findByTags?tags=friendly");
        assertEquals(200, resp.statusCode());
        List<Pet> pets = MAPPER.readValue(resp.body(), new TypeReference<>() {});
        assertFalse(pets.isEmpty());
    }

    @Test
    void updatePet() throws Exception {
        post("/api/v3/pet", buildPet(30L));
        Pet updated = buildPet(30L);
        updated.setName("Rex");
        var resp = put("/api/v3/pet", updated);
        assertEquals(200, resp.statusCode());

        var getResp = get("/api/v3/pet/30");
        Pet retrieved = MAPPER.readValue(getResp.body(), Pet.class);
        assertEquals("Rex", retrieved.getName());
    }

    @Test
    void deletePet() throws Exception {
        post("/api/v3/pet", buildPet(40L));
        var delResp = delete("/api/v3/pet/40");
        assertEquals(200, delResp.statusCode());

        var getResp = get("/api/v3/pet/40");
        assertEquals(404, getResp.statusCode());
    }

    @Test
    void missingApiKeyReturns401() throws Exception {
        var resp = http.send(
            java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(url("/api/v3/pet")))
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString("{}"))
                .build(),
            java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(401, resp.statusCode());
    }
}
