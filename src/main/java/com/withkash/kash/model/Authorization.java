package com.withkash.kash.model;

import org.joda.time.DateTime;

public class Authorization {
    public String authorizationId;
    public DateTime validUntil;
    public Authorization(String authorizationId, DateTime validUntil) {
        this.authorizationId = authorizationId;
        this.validUntil = validUntil;
    }
}
