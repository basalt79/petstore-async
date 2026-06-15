package com.petstore.user;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petstore.common.ApiResponse;
import com.petstore.user.model.User;
import io.javalin.http.Context;
import io.javalin.openapi.*;
import io.javalin.router.JavalinDefaultRoutingApi;

import java.time.Instant;
import java.util.List;

public class UserController {

    private final UserService service;
    private final ObjectMapper mapper;

    public UserController(UserService service, ObjectMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    public void register(JavalinDefaultRoutingApi routes) {
        routes.post("/api/v3/user", this::createUser);
        routes.post("/api/v3/user/createWithList", this::createUsersWithList);
        routes.get("/api/v3/user/login", this::login);
        routes.get("/api/v3/user/logout", this::logout);
        routes.get("/api/v3/user/{username}", this::getUserByName);
        routes.put("/api/v3/user/{username}", this::updateUser);
        routes.delete("/api/v3/user/{username}", this::deleteUser);
    }

    @OpenApi(
        path = "/api/v3/user", methods = {HttpMethod.POST},
        operationId = "createUser", summary = "Create user", tags = {"user"},
        requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = User.class)),
        responses = @OpenApiResponse(status = "200", content = @OpenApiContent(from = User.class))
    )
    void createUser(Context ctx) {
        User user = ctx.bodyAsClass(User.class);
        ctx.future(() -> service.createUser(user).toFuture().thenRun(() -> {
            ctx.status(200);
            ctx.json(user);
        }));
    }

    @OpenApi(
        path = "/api/v3/user/createWithList", methods = {HttpMethod.POST},
        operationId = "createUsersWithListInput", summary = "Creates list of users with given input array",
        tags = {"user"},
        requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = User[].class)),
        responses = @OpenApiResponse(status = "200", content = @OpenApiContent(from = User[].class))
    )
    void createUsersWithList(Context ctx) throws Exception {
        List<User> users = mapper.readValue(ctx.body(), new TypeReference<>() {});
        ctx.future(() -> service.createUsersWithList(users).toFuture().thenRun(() -> {
            ctx.status(200);
            ctx.json(users);
        }));
    }

    @OpenApi(
        path = "/api/v3/user/login", methods = {HttpMethod.GET},
        operationId = "loginUser", summary = "Logs user into the system", tags = {"user"},
        queryParams = {
            @OpenApiParam(name = "username", description = "The username for login"),
            @OpenApiParam(name = "password", description = "The password in clear text")
        },
        responses = {
            @OpenApiResponse(status = "200"),
            @OpenApiResponse(status = "400", description = "Invalid username/password supplied")
        }
    )
    void login(Context ctx) {
        String username = ctx.queryParam("username");
        String password = ctx.queryParam("password");
        if (username == null || password == null) {
            ctx.status(400).json(new ApiResponse(400, "error", "Username and password required"));
            return;
        }
        ctx.future(() -> service.login(username, password).toFuture().thenAccept(token -> {
            ctx.header("X-Rate-Limit", "5000");
            ctx.header("X-Expires-After", Instant.now().plusSeconds(3600).toString());
            ctx.status(200).result(token);
        }));
    }

    @OpenApi(
        path = "/api/v3/user/logout", methods = {HttpMethod.GET},
        operationId = "logoutUser", summary = "Logs out current logged in user session", tags = {"user"},
        responses = @OpenApiResponse(status = "200")
    )
    void logout(Context ctx) {
        ctx.status(200).result("ok");
    }

    @OpenApi(
        path = "/api/v3/user/{username}", methods = {HttpMethod.GET},
        operationId = "getUserByName", summary = "Get user by username", tags = {"user"},
        pathParams = @OpenApiParam(name = "username", required = true, description = "The username to fetch"),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = User.class)),
            @OpenApiResponse(status = "401", description = "Missing or invalid API key"),
            @OpenApiResponse(status = "404", description = "User not found")
        }
    )
    void getUserByName(Context ctx) {
        String username = ctx.pathParam("username");
        ctx.future(() -> service.getUserByUsername(username).toFuture().thenAccept(ctx::json));
    }

    @OpenApi(
        path = "/api/v3/user/{username}", methods = {HttpMethod.PUT},
        operationId = "updateUser", summary = "Update user", tags = {"user"},
        pathParams = @OpenApiParam(name = "username", required = true, description = "Username to update"),
        requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = User.class)),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = User.class)),
            @OpenApiResponse(status = "401", description = "Missing or invalid API key"),
            @OpenApiResponse(status = "404", description = "User not found")
        }
    )
    void updateUser(Context ctx) {
        String username = ctx.pathParam("username");
        User user = ctx.bodyAsClass(User.class);
        ctx.future(() -> service.updateUser(username, user).toFuture().thenRun(() -> {
            ctx.status(200);
            ctx.json(user);
        }));
    }

    @OpenApi(
        path = "/api/v3/user/{username}", methods = {HttpMethod.DELETE},
        operationId = "deleteUser", summary = "Delete user", tags = {"user"},
        pathParams = @OpenApiParam(name = "username", required = true, description = "Username to delete"),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = ApiResponse.class)),
            @OpenApiResponse(status = "401", description = "Missing or invalid API key"),
            @OpenApiResponse(status = "404", description = "User not found")
        }
    )
    void deleteUser(Context ctx) {
        String username = ctx.pathParam("username");
        ctx.future(() -> service.deleteUser(username).toFuture().thenRun(() -> {
            ctx.status(200);
            ctx.json(new ApiResponse(200, "unknown", "User deleted"));
        }));
    }
}
