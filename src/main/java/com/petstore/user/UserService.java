package com.petstore.user;

import com.petstore.user.model.User;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.NotFoundResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class UserService {

    private final UserRepository repo;

    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    public Mono<Void> createUser(User user) {
        if (user.getId() == null) {
            user.setId(ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE));
        }
        return repo.insert(user);
    }

    public Mono<Void> createUsersWithList(List<User> users) {
        users.forEach(u -> {
            if (u.getId() == null) {
                u.setId(ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE));
            }
        });
        return Flux.fromIterable(users)
                .flatMap(repo::insert)
                .then();
    }

    public Mono<String> login(String username, String password) {
        return repo.findByUsername(username)
                .switchIfEmpty(Mono.error(new BadRequestResponse("Invalid username/password supplied")))
                .flatMap(user -> {
                    if (!password.equals(user.getPassword())) {
                        return Mono.error(new BadRequestResponse("Invalid username/password supplied"));
                    }
                    return Mono.just("logged-in-user-session-" + username);
                });
    }

    public Mono<User> getUserByUsername(String username) {
        return repo.findByUsername(username)
                .switchIfEmpty(Mono.error(new NotFoundResponse("User not found")));
    }

    public Mono<Void> updateUser(String username, User user) {
        return repo.updateByUsername(username, user)
                .flatMap(matched -> matched
                        ? Mono.empty()
                        : Mono.error(new NotFoundResponse("User not found")));
    }

    public Mono<Void> deleteUser(String username) {
        return repo.deleteByUsername(username)
                .flatMap(deleted -> deleted
                        ? Mono.empty()
                        : Mono.error(new NotFoundResponse("User not found")));
    }
}
