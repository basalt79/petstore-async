package com.petstore.pet.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum PetStatus {
    AVAILABLE("available"),
    PENDING("pending"),
    SOLD("sold");

    private final String value;

    PetStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static PetStatus fromValue(String value) {
        for (PetStatus s : values()) {
            if (s.value.equalsIgnoreCase(value)) return s;
        }
        throw new IllegalArgumentException("Unknown pet status: " + value);
    }
}
