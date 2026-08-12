package com.sergio.planix.storage;

import com.sergio.planix.common.exception.StorageException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadDir;

    public FileStorageService(Path uploadDir) { this.uploadDir = uploadDir; }

    public String store(MultipartFile file, StorageFolder folder) {
        if (file.isEmpty()) throw new StorageException("Arquivo vazio", null);

        String ext = getExtension(file.getOriginalFilename());
        String stored = folder.path() + "/" + UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);
        try {
            Path target = resolve(stored);
            Files.createDirectories(target.getParent());
            file.transferTo(target);
            return stored;
        } catch (IOException e) {
            throw new StorageException("Falha ao salvar o arquivo", e);
        }
    }

    public Resource loadAsResource(String storedPath) {
        try {
            Resource resource = new UrlResource(resolve(storedPath).toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new StorageException("Arquivo não encontrado no disco", null);
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new StorageException("Arquivo inválido", e);
        }
    }

    public void delete(String storedPath) {
        try {
            Files.deleteIfExists(resolve(storedPath));
        } catch (IOException e) {
            throw new StorageException("Falha ao apagar o arquivo", e);
        }
    }

    private Path resolve(String storedPath) {
        Path target = uploadDir.resolve(storedPath).normalize();
        if (!target.startsWith(uploadDir)) {
            throw new StorageException("Caminho inválido", null);
        }
        return target;
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1);
    }
}
