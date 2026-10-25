package com.kasimarket.kasimarket_api.job;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

// Saves photos to a folder on this computer and reads them back.
// Later we will swap this for cloud storage (Azure Blob). Only this class changes.
@Service
public class PhotoStorageService {

    // The result of saving: the new file name and the photo type.
    public record StoredPhoto(String storedName, String contentType) {
    }

    // The folder where photos are kept.
    private final Path folder;

    // @Value reads "storage.photo-dir" from application.properties.
    public PhotoStorageService(@Value("${storage.photo-dir}") String photoDir) {
        this.folder = Path.of(photoDir).toAbsolutePath().normalize();
        try {
            // Create the folder if it does not exist yet.
            Files.createDirectories(folder);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create the photo folder: " + folder, e);
        }
    }

    // Checks that the upload really is a JPEG or PNG, then saves it.
    public StoredPhoto save(MultipartFile file) {
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not read the photo.");
        }

        // We do NOT trust the file name or type the sender gives us.
        // Instead we look at the first bytes of the file, which show its real type.
        String extension;
        String contentType;
        if (bytes.length > 4 && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
            extension = ".png";
            contentType = "image/png";
        } else if (bytes.length > 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) {
            extension = ".jpg";
            contentType = "image/jpeg";
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The photo must be a JPEG or PNG image.");
        }

        // We make up our own random file name, so the sender can never choose
        // where the file goes (this blocks "path trick" attacks).
        String storedName = UUID.randomUUID() + extension;
        try {
            Files.write(folder.resolve(storedName), bytes);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not save the photo.");
        }
        return new StoredPhoto(storedName, contentType);
    }

    // Reads a saved photo back from disk.
    public byte[] read(String storedName) {
        Path file = folder.resolve(storedName).normalize();
        // Safety check: the file must be inside our photo folder.
        if (!file.startsWith(folder)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid photo name.");
        }
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo file not found.");
        }
    }
}