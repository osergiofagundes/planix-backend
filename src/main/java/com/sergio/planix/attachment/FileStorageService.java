package com.sergio.planix.attachment;

import com.sergio.planix.common.StorageException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** Cuida do arquivo em si (o banco guarda só os metadados): salvar, carregar e apagar. */
@Service
public class FileStorageService {

    private final Path uploadDir;

    public FileStorageService(Path uploadDir) { this.uploadDir = uploadDir; }

    /** Salva o arquivo com um nome único e devolve esse nome. */
    public String store(MultipartFile file) {
        if (file.isEmpty()) throw new StorageException("Arquivo vazio", null);
        String ext = getExtension(file.getOriginalFilename());
        String stored = UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);
        try {
            Path target = uploadDir.resolve(stored).normalize();
            // proteção contra "path traversal" (../)
            if (!target.getParent().equals(uploadDir)) {
                throw new StorageException("Caminho inválido", null);
            }
            file.transferTo(target);
            return stored;
        } catch (IOException e) {
            throw new StorageException("Falha ao salvar o arquivo", e);
        }
    }

    /** Carrega o arquivo salvo como Resource para o download. */
    public Resource loadAsResource(String storedFilename) {
        try {
            Path file = uploadDir.resolve(storedFilename).normalize();
            if (!file.getParent().equals(uploadDir)) {
                throw new StorageException("Caminho inválido", null);
            }
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new StorageException("Arquivo não encontrado no disco", null);
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new StorageException("Arquivo inválido", e);
        }
    }

    public void delete(String storedFilename) {
        try {
            Path file = uploadDir.resolve(storedFilename).normalize();
            if (!file.getParent().equals(uploadDir)) {
                throw new StorageException("Caminho inválido", null);
            }
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new StorageException("Falha ao apagar o arquivo", e);
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1);
    }
}
