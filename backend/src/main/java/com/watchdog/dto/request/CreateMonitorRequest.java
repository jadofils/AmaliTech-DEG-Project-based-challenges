package com.watchdog.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateMonitorRequest {

    @NotBlank(message = "Device ID is required")
    @Size(min = 3, max = 100, message = "Device ID must be between 3 and 100 characters")
    private String id;

    @Min(value = 10, message = "Timeout must be at least 10 seconds")
    @Max(value = 86400, message = "Timeout must not exceed 86400 seconds (24 hours)")
    private int timeout;

    @NotBlank(message = "Alert email is required")
    @Email(message = "Alert email must be valid")
    private String alertEmail;

    private String alertWebhook;
}
