package com.kasimarket.kasimarket_api.vetting;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

// Saves worker documents to a private folder on this computer and reads them back.
// Later we swap this for encrypted cloud storage. Only this class will change.
@Service
public class DocumentStorageService {

    // The result of saving: the new file name and the document type.
    public record StoredDocument(String storedName, String contentType) {
    }

    private final Path folder;

    // @Value reads "storage.vetting-dir" from application.properties.
    public DocumentStorageService(@Value("${storage.vetting-dir}") String dir) {
        this.folder = Path.of(dir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(folder);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create the document folder: " + folder, e);
        }
    }

    // Checks that the upload really is a PDF, JPEG or PNG, then saves it.
    public StoredDocument save(MultipartFile file) {
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not read the document.");
        }

        // We do NOT trust the file name or type the sender gives us.
        // Instead we look at the first bytes of the file, which show its real type.
        String extension;
        String contentType;
        if (bytes.length > 4 && bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D' && bytes[3] == 'F') {
            extension = ".pdf";
            contentType = "application/pdf";
        } else if (bytes.length > 4 && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
            extension = ".png";
            contentType = "image/png";
        } else if (bytes.length > 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) {
            extension = ".jpg";
            contentType = "image/jpeg";
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The document must be a PDF, JPEG or PNG file.");
        }

        // We make up our own random file name, so the sender can never choose
        // where the file goes (this blocks "path trick" attacks).
        String storedName = UUID.randomUUID() + extension;
        try {
            Files.write(folder.resolve(storedName), bytes);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not save the document.");
        }
        return new StoredDocument(storedName, contentType);
    }

    // Reads a saved document back from disk.
    public byte[] read(String storedName) {
        Path file = folder.resolve(storedName).normalize();
        // Safety check: the file must be inside our document folder.
        if (!file.startsWith(folder)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid document name.");
        }
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document file not found.");
        }
    }
}