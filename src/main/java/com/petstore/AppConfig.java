package com.petstore;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.petstore.config.ApiKeyFilter;
import com.petstore.config.MongoConfig;
import com.petstore.pet.PetController;
import com.petstore.pet.PetRepository;
import com.petstore.pet.PetService;
import com.petstore.store.StoreController;
import com.petstore.store.StoreRepository;
import com.petstore.store.StoreService;
import com.petstore.user.UserController;
import com.petstore.user.UserRepository;
import com.petstore.user.UserService;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.swagger.SwaggerPlugin;

public class AppConfig {

    public static Javalin create(MongoConfig mongoConfig, String apiKey) {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        var petRepo = new PetRepository(mongoConfig.getDatabase(), mapper);
        var storeRepo = new StoreRepository(mongoConfig.getDatabase(), mapper);
        var userRepo = new UserRepository(mongoConfig.getDatabase(), mapper);

        var petController = new PetController(new PetService(petRepo));
        var storeController = new StoreController(new StoreService(storeRepo));
        var userController = new UserController(new UserService(userRepo), mapper);

        return Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson().updateMapper(m -> {
                m.registerModule(new JavaTimeModule());
                m.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            }));

            config.registerPlugin(new OpenApiPlugin(pluginConfig ->
                pluginConfig.withDefinitionConfiguration((version, definition) ->
                    definition.info(info -> info
                        .title("Swagger Petstore - OpenAPI 3.0")
                        .version("1.0.27")
                        .description("Javalin async + MongoDB Atlas reactive implementation of the Swagger Petstore 3 API."))
                )
            ));
            config.registerPlugin(new SwaggerPlugin());

            config.bundledPlugins.enableCors(cors -> cors.addRule(it -> {
                it.anyHost();
                it.allowCredentials = false;
            }));

            config.routes.before(new ApiKeyFilter(apiKey));
            petController.register(config.routes);
            storeController.register(config.routes);
            userController.register(config.routes);
        });
    }
}
