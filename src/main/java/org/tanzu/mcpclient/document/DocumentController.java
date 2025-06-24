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
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // Generate a unique file name to prevent conflicts
            String fileId = UUID.randomUUID().toString();

            logger.info("Uploading file {} with id {}", file.getOriginalFilename(), fileId);
            DocumentService.DocumentInfo documentInfo = documentService.storeFile(file, fileId);

            // Return the uploaded document info along with all documents
            UploadResponse response = new UploadResponse(documentInfo, documentService.getDocuments());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error uploading file {}: {}", file.getOriginalFilename(), e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse("Failed to upload file: " + e.getMessage()));
        }
    }

    @GetMapping("/documents")
    public ResponseEntity<List<DocumentService.DocumentInfo>> getDocuments() {
        try {
            return ResponseEntity.ok(documentService.getDocuments());
        } catch (Exception e) {
            logger.error("Error retrieving documents: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<?> deleteDocument(@PathVariable String documentId) {
        try {
            logger.info("Deleting document with id {}", documentId);

            boolean deleted = documentService.deleteDocument(documentId);

            if (deleted) {
                // Return updated document list after successful deletion
                DeleteResponse response = new DeleteResponse(
                        "Document deleted successfully",
                        documentService.getDocuments()
                );
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error deleting document {}: {}", documentId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse("Failed to delete document: " + e.getMessage()));
        }
    }

    @DeleteMapping("/documents")
    public ResponseEntity<?> deleteAllDocuments() {
        try {
            logger.info("Deleting all documents");
            documentService.deleteDocuments();
            return ResponseEntity.ok(new DeleteResponse("All documents deleted successfully", List.of()));
        } catch (Exception e) {
            logger.error("Error deleting all documents: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse("Failed to delete documents: " + e.getMessage()));
        }
    }

    // Response DTOs
    public record UploadResponse(
            DocumentService.DocumentInfo uploadedDocument,
            List<DocumentService.DocumentInfo> allDocuments
    ) {}

    public record DeleteResponse(
            String message,
            List<DocumentService.DocumentInfo> remainingDocuments
    ) {}

    public record ErrorResponse(String error) {}
}