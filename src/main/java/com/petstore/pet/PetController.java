package com.petstore.pet;

import com.petstore.common.ApiResponse;
import com.petstore.pet.model.Pet;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import io.javalin.openapi.*;
import io.javalin.router.JavalinDefaultRoutingApi;

import java.util.List;

public class PetController {

    private final PetService service;

    public PetController(PetService service) {
        this.service = service;
    }

    public void register(JavalinDefaultRoutingApi routes) {
        routes.put("/api/v3/pet", this::updatePet);
        routes.post("/api/v3/pet", this::addPet);
        routes.get("/api/v3/pet/findByStatus", this::findByStatus);
        routes.get("/api/v3/pet/findByTags", this::findByTags);
        routes.get("/api/v3/pet/{petId}", this::getPetById);
        routes.post("/api/v3/pet/{petId}", this::updatePetWithForm);
        routes.delete("/api/v3/pet/{petId}", this::deletePet);
        routes.post("/api/v3/pet/{petId}/uploadImage", this::uploadImage);
    }

    @OpenApi(
        path = "/api/v3/pet", methods = {HttpMethod.PUT},
        operationId = "updatePet", summary = "Update an existing pet", tags = {"pet"},
        requestBody = @OpenApiRequestBody(required = true, content = @OpenApiContent(from = Pet.class)),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = Pet.class)),
            @OpenApiResponse(status = "401", description = "Missing or invalid API key"),
            @OpenApiResponse(status = "404", description = "Pet not found")
        }
    )
    void updatePet(Context ctx) {
        Pet pet = ctx.bodyAsClass(Pet.class);
        ctx.future(() -> service.updatePet(pet).toFuture().thenAccept(ctx::json));
    }

    @OpenApi(
        path = "/api/v3/pet", methods = {HttpMethod.POST},
        operationId = "addPet", summary = "Add a new pet to the store", tags = {"pet"},
        requestBody = @OpenApiRequestBody(required = true, content = @OpenApiContent(from = Pet.class)),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = Pet.class)),
            @OpenApiResponse(status = "401", description = "Missing or invalid API key")
        }
    )
    void addPet(Context ctx) {
        Pet pet = ctx.bodyAsClass(Pet.class);
        ctx.future(() -> service.addPet(pet).toFuture().thenAccept(p -> {
            ctx.status(200);
            ctx.json(p);
        }));
    }

    @OpenApi(
        path = "/api/v3/pet/findByStatus", methods = {HttpMethod.GET},
        operationId = "findPetsByStatus", summary = "Finds pets by status", tags = {"pet"},
        queryParams = @OpenApiParam(name = "status", required = true,
            description = "Status values to filter by: available, pending, sold"),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = Pet[].class)),
            @OpenApiResponse(status = "400", description = "Invalid status value")
        }
    )
    void findByStatus(Context ctx) {
        String status = ctx.queryParam("status");
        if (status == null || status.isBlank()) {
            ctx.status(400).json(new ApiResponse(400, "error", "Missing status parameter"));
            return;
        }
        ctx.future(() -> service.findByStatus(status).toFuture().thenAccept(ctx::json));
    }

    @OpenApi(
        path = "/api/v3/pet/findByTags", methods = {HttpMethod.GET},
        operationId = "findPetsByTags", summary = "Finds pets by tags", tags = {"pet"},
        queryParams = @OpenApiParam(name = "tags", required = true,
            description = "Tags to filter by"),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = Pet[].class))
        }
    )
    void findByTags(Context ctx) {
        List<String> tags = ctx.queryParams("tags");
        ctx.future(() -> service.findByTags(tags).toFuture().thenAccept(ctx::json));
    }

    @OpenApi(
        path = "/api/v3/pet/{petId}", methods = {HttpMethod.GET},
        operationId = "getPetById", summary = "Find pet by ID", tags = {"pet"},
        pathParams = @OpenApiParam(name = "petId", required = true, type = Long.class,
            description = "ID of pet to return"),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = Pet.class)),
            @OpenApiResponse(status = "400", description = "Invalid ID supplied"),
            @OpenApiResponse(status = "404", description = "Pet not found")
        }
    )
    void getPetById(Context ctx) {
        long petId = Long.parseLong(ctx.pathParam("petId"));
        ctx.future(() -> service.getPetById(petId).toFuture().thenAccept(ctx::json));
    }

    @OpenApi(
        path = "/api/v3/pet/{petId}", methods = {HttpMethod.POST},
        operationId = "updatePetWithForm", summary = "Updates a pet in the store with form data", tags = {"pet"},
        pathParams = @OpenApiParam(name = "petId", required = true, type = Long.class,
            description = "ID of pet that needs to be updated"),
        queryParams = {
            @OpenApiParam(name = "name", description = "Updated name of the pet"),
            @OpenApiParam(name = "status", description = "Updated status of the pet")
        },
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = ApiResponse.class)),
            @OpenApiResponse(status = "401", description = "Missing or invalid API key"),
            @OpenApiResponse(status = "404", description = "Pet not found")
        }
    )
    void updatePetWithForm(Context ctx) {
        long petId = Long.parseLong(ctx.pathParam("petId"));
        String name = ctx.formParam("name");
        String status = ctx.formParam("status");
        ctx.future(() -> service.updatePetByForm(petId, name, status).toFuture().thenRun(() -> {
            ctx.status(200);
            ctx.json(new ApiResponse(200, "unknown", "Pet updated"));
        }));
    }

    @OpenApi(
        path = "/api/v3/pet/{petId}", methods = {HttpMethod.DELETE},
        operationId = "deletePet", summary = "Deletes a pet", tags = {"pet"},
        pathParams = @OpenApiParam(name = "petId", required = true, type = Long.class,
            description = "Pet id to delete"),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = ApiResponse.class)),
            @OpenApiResponse(status = "401", description = "Missing or invalid API key"),
            @OpenApiResponse(status = "404", description = "Pet not found")
        }
    )
    void deletePet(Context ctx) {
        long petId = Long.parseLong(ctx.pathParam("petId"));
        ctx.future(() -> service.deletePet(petId).toFuture().thenRun(() -> {
            ctx.status(200);
            ctx.json(new ApiResponse(200, "unknown", "Pet deleted"));
        }));
    }

    @OpenApi(
        path = "/api/v3/pet/{petId}/uploadImage", methods = {HttpMethod.POST},
        operationId = "uploadFile", summary = "Uploads an image", tags = {"pet"},
        pathParams = @OpenApiParam(name = "petId", required = true, type = Long.class,
            description = "ID of pet to update"),
        queryParams = @OpenApiParam(name = "additionalMetadata", description = "Additional metadata"),
        requestBody = @OpenApiRequestBody(content = @OpenApiContent(
            mimeType = "multipart/form-data",
            properties = @OpenApiContentProperty(name = "file", type = "string", format = "binary")
        )),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = ApiResponse.class)),
            @OpenApiResponse(status = "401", description = "Missing or invalid API key"),
            @OpenApiResponse(status = "404", description = "Pet not found")
        }
    )
    void uploadImage(Context ctx) {
        long petId = Long.parseLong(ctx.pathParam("petId"));
        var file = ctx.uploadedFile("file");
        if (file == null) {
            ctx.status(400).json(new ApiResponse(400, "unknown", "No file uploaded"));
            return;
        }
        String additionalMeta = ctx.formParam("additionalMetadata");
        String filename = file.filename();
        var stream = file.content();
        ctx.future(() -> service.uploadImage(petId, stream, filename).toFuture().thenAccept(name -> {
            ctx.status(200);
            ctx.json(new ApiResponse(200, "unknown",
                "additionalMetadata: " + additionalMeta + ", File uploaded to ./" + name + ", fieldCount: 1"));
        }));
    }
}
