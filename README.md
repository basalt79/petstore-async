# Petstore Async

[![CI](https://github.com/basalt79/petstore-async/actions/workflows/ci.yml/badge.svg)](https://github.com/basalt79/petstore-async/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-blue?logo=openjdk)
![Javalin](https://img.shields.io/badge/Javalin-7.2.2-purple)
![MongoDB](https://img.shields.io/badge/MongoDB-Atlas-green?logo=mongodb)
![Reactor](https://img.shields.io/badge/Project_Reactor-3.7.5-orange)
![License](https://img.shields.io/badge/license-MIT-blue)

A complete, production-style implementation of the [Swagger Petstore 3](https://petstore3.swagger.io/) REST API built with **Javalin 7** on **JDK 21**, backed by **MongoDB Atlas** via the **reactive streams driver**, and fully non-blocking end-to-end using **Project Reactor**.

100% API and auth compatible with [petstore-sync](https://github.com/basalt79/petstore-sync) — same routes, same request/response shapes, same Swagger UI.

---

## What's different from petstore-sync

| | petstore-sync | petstore-async |
|--|--|--|
| MongoDB driver | `mongodb-driver-sync` | `mongodb-driver-reactivestreams` |
| Repository return types | `Pet`, `List<Pet>`, `boolean` | `Mono<Pet>`, `Mono<List<Pet>>`, `Mono<Boolean>` |
| Service return types | `Pet`, `void` | `Mono<Pet>`, `Mono<Void>` |
| HTTP handlers | `ctx.json(service.get(...))` | `ctx.future(() -> service.get(...).toFuture().thenAccept(ctx::json))` |
| Thread model | one thread per request (blocking) | Jetty async, Reactor scheduler |

The API surface — URLs, HTTP methods, status codes, JSON shapes, auth headers — is identical.

---

## How async works here

### MongoDB Reactive Streams → Project Reactor → Javalin future

The MongoDB reactive streams driver returns `Publisher<T>` for every operation. Project Reactor wraps these into `Mono<T>` (single result) and `Flux<T>` (stream), which compose cleanly:

```java
// Repository
public Mono<Pet> findById(long petId) {
    return Mono.from(collection.find(Filters.eq("id", petId)).first())
               .map(this::toEntity);
}

// Service
public Mono<Pet> getPetById(long petId) {
    return repo.findById(petId)
               .switchIfEmpty(Mono.error(new NotFoundResponse("Pet not found")));
}

// Controller
void getPetById(Context ctx) {
    long petId = Long.parseLong(ctx.pathParam("petId"));
    ctx.future(() -> service.getPetById(petId).toFuture().thenAccept(ctx::json));
}
```

`Mono.toFuture()` converts to a `CompletableFuture<T>`, which Javalin's `ctx.future()` awaits on its async dispatcher — the handler thread is released immediately and the response is written when the future completes.

Errors propagate automatically: if `Mono.error(new NotFoundResponse(...))` is in the chain, the `CompletableFuture` completes exceptionally and Javalin maps it to the correct HTTP status.

### Startup: selective blocking

Index creation at startup uses `.block()` because it runs once before the server accepts requests — blocking here is intentional and safe:

```java
Mono.from(database.getCollection("pets")
    .createIndex(Indexes.ascending("id"), new IndexOptions().unique(true)))
    .block();
```

Everywhere else the code is non-blocking.

---

## Architecture

```
Controller  →  Service  →  Repository
(HTTP/future)  (Mono chain)  (Reactive MongoDB)
```

Three domains — `pet`, `store`, `user` — each self-contained with its own controller, service, repository, and models. No cross-domain dependencies.

```
src/main/java/com/petstore/
├── config/         MongoConfig, ApiKeyFilter
├── pet/            PetController, PetService, PetRepository, model/
├── store/          StoreController, StoreService, StoreRepository, model/
├── user/           UserController, UserService, UserRepository, model/
└── common/         ApiResponse
```

---

## Code-first OpenAPI

The OpenAPI spec is **generated at compile time** by an annotation processor. No hand-maintained JSON or YAML. Each handler carries `@OpenApi` metadata:

```java
@OpenApi(
    path = "/api/v3/pet", methods = {HttpMethod.POST},
    operationId = "addPet", summary = "Add a new pet to the store", tags = {"pet"},
    requestBody = @OpenApiRequestBody(required = true, content = @OpenApiContent(from = Pet.class)),
    responses = {
        @OpenApiResponse(status = "200", content = @OpenApiContent(from = Pet.class)),
        @OpenApiResponse(status = "401", description = "Missing or invalid API key")
    }
)
void addPet(Context ctx) { ... }
```

| Endpoint | Description |
|----------|-------------|
| `GET /swagger` | Interactive Swagger UI |
| `GET /openapi` | Raw OpenAPI 3.0 JSON |

---

## API key authentication

A Javalin `before` handler checks every request. Protected endpoints require either:
- `?api_key=<key>` query parameter, or
- `X-API-KEY: <key>` header

Open (no key required):

| Path | Methods |
|------|---------|
| `GET /api/v3/pet/{petId}` | public read |
| `GET /api/v3/store/order/{orderId}` | public read |
| `GET /api/v3/pet/findByStatus` | public |
| `GET /api/v3/pet/findByTags` | public |
| `GET /api/v3/store/inventory` | public |
| `GET /api/v3/user/login` | public |
| `GET /api/v3/user/logout` | public |
| `POST /api/v3/user` | public |
| `POST /api/v3/user/createWithList` | public |
| `/swagger*`, `/openapi*`, `/webjars*` | public |

---

## Pet photos

`POST /api/v3/pet/{petId}/uploadImage` accepts a `multipart/form-data` file upload. The image is base64-encoded and stored in the `photoBase64` field of the pet document. It is returned in all GET responses once set. A full PUT update preserves the existing `photoBase64` (it is never cleared by a metadata update).

---

## Integration tests — real MongoDB, no mocks

Tests use **Testcontainers 2.0.5** to spin up a real MongoDB 7.0 container. The reactive test base clears collections between tests using `.block()`:

```java
@BeforeEach void setUp() {
    Mono.from(db.getCollection("pets").deleteMany(new Document())).block();
    // ...
    app = AppConfig.create(mongoConfig, API_KEY);
    app.start(0); // random port
}
```

**17 integration tests** covering all three domains, happy paths, 404s, and 401s.

---

## Quick start

```bash
# 1. Build fat JAR
mvn clean package -DskipTests

# 2. Set environment
export MONGO_URI="mongodb+srv://user:pass@cluster.mongodb.net/"
export API_KEY="special-key"
export PORT=7070   # optional, default 7070

# 3. Run
java -jar target/petstore-async-1.0-SNAPSHOT-jar-with-dependencies.jar
```

Or use the helper script:

```bash
chmod +x run.sh
MONGO_URI="..." API_KEY="..." ./run.sh
```

Open **http://localhost:7070/swagger** — click Authorize, enter your API key, and start calling endpoints.

---

## Stack

| | |
|--|--|
| Language | Java 21 |
| Framework | Javalin 7.2.2 |
| Database | MongoDB Atlas (`mongodb-driver-reactivestreams` 5.5.1) |
| Async | Project Reactor 3.7.5 |
| JSON | Jackson 2.19.0 |
| API docs | `javalin-openapi-plugin` 7.2.2 + `javalin-swagger-plugin` 7.2.2 |
| Tests | JUnit 5.12.2 + Testcontainers 2.0.5 |
| Build | Maven 3.9+ |

---

## Related

- [petstore-sync](https://github.com/basalt79/petstore-sync) — identical API, synchronous blocking implementation
