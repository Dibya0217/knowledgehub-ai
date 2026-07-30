package com.dibya.knowledgehub.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

public interface FileStorageService {
    String store(MultipartFile file, UUID userId, UUID documentId) throws IOException;
    Resource load(String storagePath) throws IOException;
    void delete(String storagePath) throws IOException;
}
