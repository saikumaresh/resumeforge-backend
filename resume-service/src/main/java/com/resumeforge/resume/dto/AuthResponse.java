package com.resumeforge.resume.dto;

import java.util.UUID;

public class AuthResponse {

    private String token;
    private UUID   userId;
    private String name;
    private String email;
    private String plan;

    public AuthResponse(String token, UUID userId, String name, String email, String plan) {
        this.token  = token;
        this.userId = userId;
        this.name   = name;
        this.email  = email;
        this.plan   = plan != null ? plan : "FREE";
    }

    public String getToken()   { return token; }
    public UUID   getUserId()  { return userId; }
    public String getName()    { return name; }
    public String getEmail()   { return email; }
    public String getPlan()    { return plan; }
}
