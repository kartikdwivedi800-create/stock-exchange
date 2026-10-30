package com.nexusexchange.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds {@code nexus.admin} (env ADMIN_USERNAME / ADMIN_PASSWORD). Both empty means "no admin seeding". */
@ConfigurationProperties("nexus.admin")
public record AdminProperties(String username, String password) {

    public boolean isConfigured() {
        return username != null && !username.isBlank() && password != null && !password.isBlank();
    }

    /** Never print the password, even by accident. */
    @Override
    public String toString() {
        return "AdminProperties[username=" + username + ", password=****]";
    }
}
