package com.menzo.menzo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cloudinary.Cloudinary;

@Configuration
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary(CloudinaryProperties properties) {
        if (properties.getUrl() == null || properties.getUrl().isBlank()) {
            // Sin CLOUDINARY_URL configurada (p. ej. en desarrollo local): instancia inactiva,
            // solo falla si de verdad se intenta subir un archivo.
            return new Cloudinary();
        }
        return new Cloudinary(properties.getUrl());
    }
}
