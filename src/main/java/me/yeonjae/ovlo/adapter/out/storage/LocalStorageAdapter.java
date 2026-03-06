package me.yeonjae.ovlo.adapter.out.storage;

import me.yeonjae.ovlo.application.port.out.media.StoragePort;
import me.yeonjae.ovlo.domain.media.model.MediaType;
import me.yeonjae.ovlo.domain.media.model.StoragePath;
import me.yeonjae.ovlo.domain.media.model.StorageType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
public class LocalStorageAdapter implements StoragePort {

    private final String uploadDir;

    public LocalStorageAdapter(@Value("${ovlo.storage.local.path:/tmp/ovlo-media}") String uploadDir) {
        this.uploadDir = uploadDir;
    }

    @Override
    public StoragePath store(byte[] data, MediaType type, String originalFilename) {
        try {
            Path dirPath = Paths.get(uploadDir);
            Files.createDirectories(dirPath);
            String extension = getExtension(originalFilename);
            String filename = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);
            Path filePath = dirPath.resolve(filename);
            Files.write(filePath, data);
            return new StoragePath(filePath.toString(), StorageType.LOCAL);
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 실패: " + originalFilename, e);
        }
    }

    @Override
    public byte[] retrieve(StoragePath path) {
        try {
            return Files.readAllBytes(Paths.get(path.path()));
        } catch (IOException e) {
            throw new RuntimeException("파일 조회 실패: " + path.path(), e);
        }
    }

    @Override
    public void delete(StoragePath path) {
        try {
            Files.deleteIfExists(Paths.get(path.path()));
        } catch (IOException e) {
            throw new RuntimeException("파일 삭제 실패: " + path.path(), e);
        }
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex >= 0 ? filename.substring(dotIndex + 1) : "";
    }
}
