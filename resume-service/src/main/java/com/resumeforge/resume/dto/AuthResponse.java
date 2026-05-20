package com.resumeforge.resume.dto;

import java.util.UUID;

public class AuthResponse {

    private String token;
    private UUID   userId;
    private String name;
    private String email;

    public AuthResponse(String token, UUID userId, String name, String email) {
        this.token  = token;
        this.userId = userId;
        this.name   = name;
        this.email  = email;
    }

    public String getToken()  { return token; }
    public UUID   getUserId() { return userId; }
    public String getName()   { return name; }
    public String getEmail()  { return email; }
}
