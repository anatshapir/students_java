package com.javaedu.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "sandbox")
@Getter
@Setter
public class SandboxConfig {

    private int timeoutSeconds = 30;
    private int maxMemoryMb = 256;
    private int maxThreads = 2;
    private List<String> allowedPackages = List.of(
            "java.lang",
            "java.util",
            "java.math",
            "java.text",
            "java.time"
    );
}
