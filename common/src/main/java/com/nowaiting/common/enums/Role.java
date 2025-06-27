package com.nowaiting.common.enums;

public enum Role {
    USER("USER"),
    MANAGER("MANAGER");

    private final String name;

    Role(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static Role fromString(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Role name cannot be null or empty");
        }
        for (Role role : Role.values()){
            if (role.name.equalsIgnoreCase(name)){
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + name);
    }
}
