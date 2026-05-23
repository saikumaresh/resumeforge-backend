package com.resumeforge.resume.dto;

import java.util.UUID;

public class AuthResponse {

    private String token;
    private UUID   userId;
    private String name;
    private String email;
    private String plan;
    private String pictureUrl;

    public AuthResponse(String token, UUID userId, String name, String email, String plan, String pictureUrl) {
        this.token      = token;
        this.userId     = userId;
        this.name       = name;
        this.email      = email;
        this.plan       = plan != null ? plan : "FREE";
        this.pictureUrl = pictureUrl;
    }

    public String getToken()       { return token; }
    public UUID   getUserId()      { return userId; }
    public String getName()        { return name; }
    public String getEmail()       { return email; }
    public String getPlan()        { return plan; }
    public String getPictureUrl()  { return pictureUrl; }
}
