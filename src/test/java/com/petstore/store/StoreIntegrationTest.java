package com.petstore.store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.petstore.BaseIntegrationTest;
import com.petstore.store.model.Order;
import com.petstore.store.model.OrderStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StoreIntegrationTest extends BaseIntegrationTest {

    private Order buildOrder(long id) {
        Order order = new Order();
        order.setId(id);
        order.setPetId(1L);
        order.setQuantity(1);
        order.setShipDate(Instant.now());
        order.setStatus(OrderStatus.PLACED);
        order.setComplete(false);
        return order;
    }

    @Test
    void placeAndGetOrder() throws Exception {
        var resp = post("/api/v3/store/order", buildOrder(1L));
        assertEquals(200, resp.statusCode());

        var getResp = get("/api/v3/store/order/1");
        assertEquals(200, getResp.statusCode());
        Order retrieved = MAPPER.readValue(getResp.body(), Order.class);
        assertEquals(1L, retrieved.getId());
        assertEquals("placed", retrieved.getStatus().getValue());
    }

    @Test
    void getOrderNotFound() throws Exception {
        var resp = get("/api/v3/store/order/999");
        assertEquals(404, resp.statusCode());
    }

    @Test
    void deleteOrder() throws Exception {
        post("/api/v3/store/order", buildOrder(2L));
        var delResp = delete("/api/v3/store/order/2");
        assertEquals(200, delResp.statusCode());

        var getResp = get("/api/v3/store/order/2");
        assertEquals(404, getResp.statusCode());
    }

    @Test
    void getInventory() throws Exception {
        post("/api/v3/store/order", buildOrder(3L));
        Order approved = buildOrder(4L);
        approved.setStatus(OrderStatus.APPROVED);
        post("/api/v3/store/order", approved);

        var resp = get("/api/v3/store/inventory");
        assertEquals(200, resp.statusCode());
        Map<String, Integer> inventory = MAPPER.readValue(resp.body(), new TypeReference<>() {});
        assertEquals(1, inventory.get("placed"));
        assertEquals(1, inventory.get("approved"));
    }
}
