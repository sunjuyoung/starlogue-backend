package com.example.starlogue.config.websocket;

import java.security.Principal;
import java.util.UUID;

public record StompPrincipal(UUID userId, String email) implements Principal {

    @Override
    public String getName() {
        return userId.toString();
    }
}
