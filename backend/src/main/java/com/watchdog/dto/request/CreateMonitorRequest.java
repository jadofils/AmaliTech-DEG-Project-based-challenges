// dto/request/CreateMonitorRequest.java
package com.watchdog.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMonitorRequest {
    
    @NotBlank(message = "Device ID is required")
    @Pattern(regexp = "^[a-zA-Z0-9-_]+$", 
             message = "Device ID can only contain letters, numbers, hyphens, and underscores")
    @Size(min = 3, max = 100, message = "Device ID must be between 3 and 100 characters")
    @JsonProperty("id")
    private String deviceId;
    
    @NotNull(message = "Timeout is required")
    @Min(value = 10, message = "Timeout must be at least 10 seconds")
    @Max(value = 86400, message = "Timeout cannot exceed 24 hours (86400 seconds)")
    private Integer timeout;
    
    @NotBlank(message = "Alert email is required")
    @Email(message = "Invalid email format")
    @JsonProperty("alert_email")
    private String alertEmail;
    
    @JsonProperty("alert_webhook")
    @Schema(description = "Optional webhook URL to receive alert notifications", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "")
    private String alertWebhook;
}