package com.bookkaro.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class RateLimitResponse {
    private int status;
    private String error;
    private String message;
    private String path;
    private long retryAfterSeconds;
}
