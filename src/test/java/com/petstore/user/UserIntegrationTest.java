package com.petstore.user;

import com.petstore.BaseIntegrationTest;
import com.petstore.user.model.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserIntegrationTest extends BaseIntegrationTest {

    private User buildUser(String username) {
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john@example.com");
        user.setPassword("password123");
        user.setPhone("555-0100");
        user.setUserStatus(1);
        return user;
    }

    @Test
    void createAndGetUser() throws Exception {
        var createResp = post("/api/v3/user", buildUser("johndoe"));
        assertEquals(200, createResp.statusCode());

        var getResp = getWithKey("/api/v3/user/johndoe");
        assertEquals(200, getResp.statusCode());
        User retrieved = MAPPER.readValue(getResp.body(), User.class);
        assertEquals("johndoe", retrieved.getUsername());
        assertEquals("John", retrieved.getFirstName());
    }

    @Test
    void getUserNotFound() throws Exception {
        var resp = getWithKey("/api/v3/user/nobody");
        assertEquals(404, resp.statusCode());
    }

    @Test
    void login() throws Exception {
        post("/api/v3/user", buildUser("testuser"));
        var resp = get("/api/v3/user/login?username=testuser&password=password123");
        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("testuser"));
    }

    @Test
    void loginInvalidCredentials() throws Exception {
        post("/api/v3/user", buildUser("testuser2"));
        var resp = get("/api/v3/user/login?username=testuser2&password=wrong");
        assertEquals(400, resp.statusCode());
    }

    @Test
    void updateUser() throws Exception {
        post("/api/v3/user", buildUser("updatable"));
        User updated = buildUser("updatable");
        updated.setFirstName("Jane");
        var resp = put("/api/v3/user/updatable", updated);
        assertEquals(200, resp.statusCode());

        var getResp = getWithKey("/api/v3/user/updatable");
        User retrieved = MAPPER.readValue(getResp.body(), User.class);
        assertEquals("Jane", retrieved.getFirstName());
    }

    @Test
    void deleteUser() throws Exception {
        post("/api/v3/user", buildUser("deleteme"));
        var delResp = delete("/api/v3/user/deleteme");
        assertEquals(200, delResp.statusCode());

        var getResp = getWithKey("/api/v3/user/deleteme");
        assertEquals(404, getResp.statusCode());
    }
}
