package com.sergio.planix.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class StorageConfig {

    @Bean
    Path uploadDir(@Value("${planix.upload-dir}") String dir) throws IOException {
        Path path = Paths.get(dir).toAbsolutePath().normalize();
        Files.createDirectories(path);     // cria ./uploads se não existir
        return path;
    }
}
