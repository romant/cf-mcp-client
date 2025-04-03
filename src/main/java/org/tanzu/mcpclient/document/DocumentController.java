package org.tanzu.mcpclient.document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
public class DocumentController {
    private final DocumentService documentService;
    private final List<DocumentInfo> documentList = new ArrayList<>();

    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/upload")
    public ResponseEntity<DocumentInfo> uploadFile(@RequestParam("file") MultipartFile file) {
        // Generate a unique file name to prevent conflicts
        String fileId = UUID.randomUUID().toString();
        String originalFileName = file.getOriginalFilename();

        logger.info("Uploading file " + originalFileName + " with id " + fileId);

        documentService.writeToVectorStore(file, fileId);

        DocumentInfo documentInfo = new DocumentInfo(
                fileId,
                originalFileName != null ? originalFileName : "Unknown",
                file.getSize(),
                Instant.now().toString()
        );

        documentList.clear();
        documentList.add(documentInfo);

        return ResponseEntity.ok(documentInfo);
    }

    @GetMapping("/documents")
    public ResponseEntity<List<DocumentInfo>> getDocuments() {
        return ResponseEntity.ok(documentList);
    }

    @DeleteMapping("/documents/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable String id) {
        DocumentInfo documentInfo = documentList.stream()
                .filter(doc -> doc.id().equals(id))
                .findFirst()
                .orElse(null);

        if (documentInfo == null) {
            return ResponseEntity.notFound().build();
        }

        documentList.removeIf(doc -> doc.id().equals(id));

        return ResponseEntity.ok().build();
    }


    public record DocumentInfo(String id, String name, long size, String uploadDate) {
    }
}
