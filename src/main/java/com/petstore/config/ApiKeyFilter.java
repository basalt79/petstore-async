package com.petstore.config;

import com.petstore.common.ApiResponse;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpResponseException;

import java.util.Set;

public class ApiKeyFilter implements Handler {

    private static final Set<String> OPEN_PATHS = Set.of(
            "/api/v3/pet/findByStatus",
            "/api/v3/pet/findByTags",
            "/api/v3/store/inventory",
            "/api/v3/user/login",
            "/api/v3/user/logout",
            "/api/v3/user",
            "/api/v3/user/createWithList"
    );

    private final String validApiKey;

    public ApiKeyFilter(String validApiKey) {
        this.validApiKey = validApiKey;
    }

    @Override
    public void handle(Context ctx) {
        if (ctx.method() == HandlerType.OPTIONS) {
            var origin = ctx.header("Origin");
            if (origin == null) {
                origin = "*";
            }

            ctx.header("Access-Control-Allow-Origin", origin);
            ctx.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS");
            ctx.header("Access-Control-Allow-Headers", "Content-Type, Authorization, X-API-KEY, api_key, X-Requested-With");
            ctx.header("Access-Control-Allow-Credentials", "true");

            ctx.status(200);
            return;
        }
        var path = ctx.path();
        var method = ctx.method().name();
        if ("GET".equals(method)) {
            if (path.matches("/api/v3/pet/\\d+")
                    || path.matches("/api/v3/store/order/\\d+")) {
                return;
            }
        }

        if (path.startsWith("/swagger") || path.startsWith("/openapi") || path.startsWith("/webjars")) {
            return;
        }

        if (OPEN_PATHS.contains(path)) {
            return;
        }

        var key = ctx.queryParam("api_key");
        if (key == null) {
            key = ctx.header("X-API-KEY");
        }

        if (validApiKey == null || !validApiKey.equals(key)) {
            throw new HttpResponseException(401, "Missing or invalid API key");
        }
    }
}
