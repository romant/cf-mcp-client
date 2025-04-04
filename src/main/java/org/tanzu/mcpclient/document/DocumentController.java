package org.tanzu.mcpclient.document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
public class DocumentController {
    private final DocumentService documentService;

    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/upload")
    public ResponseEntity<DocumentService.DocumentInfo> uploadFile(@RequestParam("file") MultipartFile file) {
        // Generate a unique file name to prevent conflicts
        String fileId = UUID.randomUUID().toString();

        logger.info("Uploading file " + file.getOriginalFilename() + " with id " + fileId);
        DocumentService.DocumentInfo documentInfo = documentService.storeFile(file, fileId);

        return ResponseEntity.ok(documentInfo);
    }

    @GetMapping("/documents")
    public ResponseEntity<List<DocumentService.DocumentInfo>> getDocuments() {
        return ResponseEntity.ok(documentService.getDocuments());
    }

    @DeleteMapping("/documents")
    public ResponseEntity<Void> deleteDocuments() {

        documentService.deleteDocuments();
        return ResponseEntity.ok().build();
    }
}
