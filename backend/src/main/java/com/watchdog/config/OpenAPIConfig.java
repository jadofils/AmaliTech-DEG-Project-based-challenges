// config/OpenAPIConfig.java
package com.watchdog.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
public class OpenAPIConfig {
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Watchdog Sentinel API")
                .version("1.0.0")
                .description("Dead Man's Switch API for monitoring device heartbeats")
                .contact(new Contact()
                    .name("CritMon Support")
                    .email("support@critmon.com"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")))
            .tags(List.of(
                new Tag().name("Monitor Management").description("Core monitor endpoints"),
                new Tag().name("Audit Logs").description("Audit trail endpoints")
            ));
    }
}