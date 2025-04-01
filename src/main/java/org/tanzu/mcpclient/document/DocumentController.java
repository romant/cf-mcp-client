package org.tanzu.mcpclient.document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.tanzu.mcpclient.chat.ChatConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
public class DocumentController {
    private final Path fileStorageLocation;
    private final List<DocumentInfo> documentList = new ArrayList<>();

    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);

    public DocumentController() throws IOException {
        this.fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();
        Files.createDirectories(this.fileStorageLocation);
    }

    @PostMapping("/upload")
    public ResponseEntity<DocumentInfo> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // Generate a unique file name to prevent conflicts
            String fileId = UUID.randomUUID().toString();
            String originalFileName = file.getOriginalFilename();
            Path targetLocation = this.fileStorageLocation.resolve(fileId);

            logger.info("Uploading file " + originalFileName + " to " + targetLocation);
            if (originalFileName.equals("Corby")) {
                Files.copy(file.getInputStream(), targetLocation);
            }

            DocumentInfo documentInfo = new DocumentInfo(
                    fileId,
                    originalFileName != null ? originalFileName : "Unknown",
                    file.getSize(),
                    Instant.now().toString()
            );

            documentList.add(documentInfo);

            return ResponseEntity.ok(documentInfo);
        } catch (IOException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/documents")
    public ResponseEntity<List<DocumentInfo>> getDocuments() {
        return ResponseEntity.ok(documentList);
    }

    @DeleteMapping("/documents/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable String id) {
        try {
            DocumentInfo documentInfo = documentList.stream()
                    .filter(doc -> doc.id().equals(id))
                    .findFirst()
                    .orElse(null);

            if (documentInfo == null) {
                return ResponseEntity.notFound().build();
            }

            Path filePath = fileStorageLocation.resolve(id);
            Files.deleteIfExists(filePath);

            documentList.removeIf(doc -> doc.id().equals(id));

            return ResponseEntity.ok().build();
        } catch (IOException ex) {
            return ResponseEntity.badRequest().build();
        }
    }


    public record DocumentInfo(String id, String name, long size, String uploadDate) {
    }
}
