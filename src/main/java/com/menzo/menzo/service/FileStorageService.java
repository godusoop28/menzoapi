package com.menzo.menzo.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.menzo.menzo.config.UploadsProperties;
import com.menzo.menzo.exception.BadRequestException;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;

    private final UploadsProperties uploadsProperties;

    public FileStorageService(UploadsProperties uploadsProperties) {
        this.uploadsProperties = uploadsProperties;
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("El archivo está vacío");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BadRequestException("El archivo supera el tamaño máximo permitido (10MB)");
        }

        String extension = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("Formato de imagen no soportado: " + extension);
        }

        try {
            Path dir = Path.of(uploadsProperties.getDir()).toAbsolutePath().normalize();
            Files.createDirectories(dir);

            String filename = UUID.randomUUID() + "." + extension;
            Path target = dir.resolve(filename);

            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target);
            }

            return uploadsProperties.getPublicBaseUrl() + "/" + filename;
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar el archivo", e);
        }
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new BadRequestException("El archivo debe tener una extensión válida");
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
    }
}
