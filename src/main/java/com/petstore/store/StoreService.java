package com.petstore.store;

import com.petstore.store.model.Order;
import io.javalin.http.NotFoundResponse;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class StoreService {

    private final StoreRepository repo;

    public StoreService(StoreRepository repo) {
        this.repo = repo;
    }

    public Mono<Order> placeOrder(Order order) {
        if (order.getId() == null) {
            order.setId(ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE));
        }
        return repo.insert(order).thenReturn(order);
    }

    public Mono<Order> getOrderById(long orderId) {
        return repo.findById(orderId)
                .switchIfEmpty(Mono.error(new NotFoundResponse("Order not found")));
    }

    public Mono<Void> deleteOrder(long orderId) {
        return repo.deleteById(orderId)
                .flatMap(deleted -> deleted
                        ? Mono.empty()
                        : Mono.error(new NotFoundResponse("Order not found")));
    }

    public Mono<Map<String, Integer>> getInventory() {
        return repo.getInventoryCounts();
    }
}
