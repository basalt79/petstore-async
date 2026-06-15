package com.petstore;

import com.petstore.config.MongoConfig;

public class Main {

    public static void main(String[] args) {
        var mongoUri = System.getenv("MONGO_URI");
        if (mongoUri == null || mongoUri.isBlank()) {
            System.err.println("MONGO_URI environment variable is required");
            System.exit(1);
        }

        var apiKey = System.getenv("API_KEY");
        int port = parsePort(System.getenv("PORT"), 7070);

        var mongoConfig = new MongoConfig(mongoUri);
        Runtime.getRuntime().addShutdownHook(new Thread(mongoConfig::close));

        AppConfig.create(mongoConfig, apiKey).start(port);
    }

    private static int parsePort(String value, int defaultPort) {
        if (value == null || value.isBlank()) return defaultPort;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultPort;
        }
    }
}
