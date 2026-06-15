package com.petstore.store;

import com.petstore.common.ApiResponse;
import com.petstore.store.model.Order;
import io.javalin.http.Context;
import io.javalin.openapi.*;
import io.javalin.router.JavalinDefaultRoutingApi;

public class StoreController {

    private final StoreService service;

    public StoreController(StoreService service) {
        this.service = service;
    }

    public void register(JavalinDefaultRoutingApi routes) {
        routes.get("/api/v3/store/inventory", this::getInventory);
        routes.post("/api/v3/store/order", this::placeOrder);
        routes.get("/api/v3/store/order/{orderId}", this::getOrderById);
        routes.delete("/api/v3/store/order/{orderId}", this::deleteOrder);
    }

    @OpenApi(
        path = "/api/v3/store/inventory", methods = {HttpMethod.GET},
        operationId = "getInventory", summary = "Returns pet inventories by status", tags = {"store"},
        responses = @OpenApiResponse(status = "200")
    )
    void getInventory(Context ctx) {
        ctx.future(() -> service.getInventory().toFuture().thenAccept(ctx::json));
    }

    @OpenApi(
        path = "/api/v3/store/order", methods = {HttpMethod.POST},
        operationId = "placeOrder", summary = "Place an order for a pet", tags = {"store"},
        requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = Order.class)),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = Order.class)),
            @OpenApiResponse(status = "401", description = "Missing or invalid API key")
        }
    )
    void placeOrder(Context ctx) {
        Order order = ctx.bodyAsClass(Order.class);
        ctx.future(() -> service.placeOrder(order).toFuture().thenAccept(o -> {
            ctx.status(200);
            ctx.json(o);
        }));
    }

    @OpenApi(
        path = "/api/v3/store/order/{orderId}", methods = {HttpMethod.GET},
        operationId = "getOrderById", summary = "Find purchase order by ID", tags = {"store"},
        pathParams = @OpenApiParam(name = "orderId", required = true, type = Long.class,
            description = "ID of order to fetch"),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = Order.class)),
            @OpenApiResponse(status = "400", description = "Invalid ID supplied"),
            @OpenApiResponse(status = "404", description = "Order not found")
        }
    )
    void getOrderById(Context ctx) {
        long orderId = Long.parseLong(ctx.pathParam("orderId"));
        ctx.future(() -> service.getOrderById(orderId).toFuture().thenAccept(ctx::json));
    }

    @OpenApi(
        path = "/api/v3/store/order/{orderId}", methods = {HttpMethod.DELETE},
        operationId = "deleteOrder", summary = "Delete purchase order by ID", tags = {"store"},
        pathParams = @OpenApiParam(name = "orderId", required = true, type = Long.class,
            description = "ID of the order to delete"),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = ApiResponse.class)),
            @OpenApiResponse(status = "401", description = "Missing or invalid API key"),
            @OpenApiResponse(status = "404", description = "Order not found")
        }
    )
    void deleteOrder(Context ctx) {
        long orderId = Long.parseLong(ctx.pathParam("orderId"));
        ctx.future(() -> service.deleteOrder(orderId).toFuture().thenRun(() -> {
            ctx.status(200);
            ctx.json(new ApiResponse(200, "unknown", "Order deleted"));
        }));
    }
}
