package com.dibya.knowledgehub.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    private final Path baseDir;

    public LocalFileStorageService(@Value("${app.storage.location:./uploads}") String uploadDir) {
        this.baseDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public String store(MultipartFile file, UUID userId, UUID documentId) throws IOException {
        String originalFilename = file.getOriginalFilename() != null
                ? file.getOriginalFilename()
                : "file";
        Path targetDir = baseDir.resolve(userId.toString()).resolve(documentId.toString());
        Files.createDirectories(targetDir);
        Path targetPath = targetDir.resolve(originalFilename);
        file.transferTo(targetPath);
        return baseDir.relativize(targetPath).toString();
    }

    @Override
    public Resource load(String storagePath) throws MalformedURLException {
        Path filePath = baseDir.resolve(storagePath).normalize();
        return new UrlResource(filePath.toUri());
    }

    @Override
    public void delete(String storagePath) throws IOException {
        Path filePath = baseDir.resolve(storagePath).normalize();
        Files.deleteIfExists(filePath);
    }
}
