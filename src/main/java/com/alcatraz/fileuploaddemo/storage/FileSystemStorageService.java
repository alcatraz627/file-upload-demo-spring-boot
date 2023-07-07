package com.alcatraz.fileuploaddemo.storage;

import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FileSystemStorageService implements StorageService {
    private final Path rootLocation;

    @Autowired
    public FileSystemStorageService(StorageProperties properties) {
        this.rootLocation = Paths.get(properties.getLocation());
    }

    public void store(MultipartFile file) {
        var filename = file.getOriginalFilename();

        store(file, filename);
    }

    @Override
    public void store(MultipartFile file, String filename) {
        try {
            if (file.isEmpty()) {
                throw new StorageException("Failed to store empty file " + file.getOriginalFilename());
            }

            UUID folderName = UUID.randomUUID();
            Files.createDirectory(this.rootLocation.resolve(folderName.toString()));

            Path destinationFile = this.rootLocation.resolve(folderName.toString()).resolve(Paths.get(filename).normalize()).toAbsolutePath();

            if (!destinationFile.getParent().getParent().equals(this.rootLocation.toAbsolutePath())) {
                throw new StorageException("Cannot store file outside current directory.");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new StorageException("Failed to store file " + file.getOriginalFilename(), e);
        }
    }

    @Override
    public Stream<Path> loadAll() {
        Stream<Path> result;
        try {
            result = Files.walk(this.rootLocation, 3)
                    .filter(path -> !path.equals(this.rootLocation))
                    .filter(Files::isDirectory)
                    .map(this.rootLocation::relativize);
            return result;
        } catch (IOException e) {
            throw new StorageException("Failed to read stored files", e);
        }
    }

    @Override
    public Path load(String filename) {
        return rootLocation.resolve(filename);
    }

    @Override
    public Resource loadAsResource(String uuid) throws StorageFileNotFoundException {
        try {
            Path file;
            try {
                var folderPath = rootLocation.resolve(uuid);
                var filename = Files.walk(folderPath).filter(Files::isRegularFile).map(Path::getFileName).map(Path::toString).collect(Collectors.toList()).get(0);
                file = load(uuid + "/" + filename);
            } catch (IOException e) {
                throw new StorageFileNotFoundException("Could not read file: " + uuid);
            }

            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new StorageFileNotFoundException("Could not read file: " + uuid);
            }
        } catch (MalformedURLException e) {
            throw new StorageFileNotFoundException("Could not read file: " + uuid, e);
        }
    }

    @Override
    public void deleteAll() {
        FileSystemUtils.deleteRecursively(rootLocation.toFile());
    }

    @Override
    public void init() {
        try {
            var path = Files.createDirectories(rootLocation);
            System.out.println("path: " + path);
        } catch (IOException e) {
            throw new StorageException("Could not initialize storage", e);
        }
    }
}
