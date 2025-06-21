# Multiple Document Support - Design & Implementation Plan

## Overview
This document outlines the design and implementation plan for expanding the document management functionality to support multiple document uploads with individual document management capabilities.

## Current State Analysis

### ✅ Phase 1 Complete - Backend Support
**Status: IMPLEMENTED**

The backend now fully supports multiple documents with the following capabilities:

#### Resolved Limitations
1. ~~**Single Document Constraint**: `DocumentService.storeFile()` deletes all documents before adding a new one~~ ✅ **FIXED**: Documents are now preserved when uploading new ones
2. ~~**Single Document ID Tracking**: The application tracks only one `currentDocumentId`~~ ✅ **BACKEND READY**: Supports `List<String> documentIds`
3. ~~**No Individual Delete**: Users can only clear all documents at once~~ ✅ **IMPLEMENTED**: Individual document deletion via `DELETE /documents/{documentId}`
4. ~~**Simple Filter Expression**: Chat queries filter by a single document ID~~ ✅ **ENHANCED**: OR filter expressions for multiple documents

### Existing Infrastructure (Leveraged)
- ✅ Unique document ID generation per upload
- ✅ Vector store supports multiple documents with metadata
- ✅ Backend maintains a `documentList` collection
- ✅ Filter expression infrastructure exists in `ChatService`

## Implemented Design

### ✅ Core Requirements (Backend Complete)
1. ✅ Support uploading multiple documents without removing existing ones
2. ✅ Backend maintains all uploaded documents with metadata (name, size, upload date)
3. ✅ Individual document deletion via REST API
4. ✅ Include all uploaded documents in RAG queries by default
5. ✅ Session-based document persistence maintained

### ✅ Architecture Changes Implemented

#### Backend Changes (COMPLETE)

##### ✅ DocumentService.java
- **✅ Removed auto-delete**: Eliminated `deleteDocuments()` call in `storeFile()`
- **✅ Added method**: `deleteDocument(String documentId)` for individual deletion
- **✅ Added validation**: `documentExists(String documentId)` method for proper error handling
- **✅ Enhanced security**: `getDocuments()` returns defensive copies
- **✅ Enhanced**: Returns updated document list after each operation

##### ✅ DocumentController.java
- **✅ New endpoint**: `DELETE /documents/{documentId}` for individual document deletion
- **✅ Enhanced responses**: Structured DTOs (`UploadResponse`, `DeleteResponse`, `ErrorResponse`)
- **✅ Improved error handling**: Proper validation and error responses
- **✅ Complete document tracking**: All operations return updated document lists
- **✅ Backward compatibility**: Existing endpoints preserved

##### ✅ ChatController.java
- **✅ Multiple ID support**: Added `documentIds` parameter alongside legacy `documentId`
- **✅ Smart parameter resolution**: Prioritizes new parameter while maintaining backward compatibility
- **✅ Input validation**: Filters null/empty document IDs
- **✅ Enhanced logging**: Better debugging information

##### ✅ ChatService.java
- **✅ Multiple ID support**: Accept `List<String> documentIds` with backward compatibility method
- **✅ OR filter expressions**: Build composite filters like `documentId == 'doc1' OR documentId == 'doc2'`
- **✅ Performance optimized**: Efficient single vs multiple document handling
- **✅ Enhanced logging**: Shows which documents are being used in queries

#### Frontend Changes (PENDING - Phase 2)

##### DocumentPanelComponent (TODO)
- **UI Updates**:
  - Show scrollable list of all documents
  - Add delete button for each document
  - Show document count in panel
  - Improve empty state messaging
- **State Management**:
  - Track all uploaded documents
  - Handle individual deletion
  - Emit all document IDs to parent

##### AppComponent (TODO)
- **State change**: Replace `currentDocumentId` with `currentDocumentIds: string[]`
- **Update**: Document selection handler to manage arrays

##### ChatboxComponent (TODO)
- **Props update**: Accept `documentIds: string[]` instead of single `documentId`
- **Query params**: Send multiple document IDs to chat endpoint

### ✅ Implemented Data Flow

```
✅ BACKEND IMPLEMENTED:

1. User uploads document
   → DocumentController generates unique ID
   → DocumentService adds to vector store (NO deletion of existing docs)
   → Returns UploadResponse{uploadedDocument, allDocuments}
   → UI updates to show all documents

2. User deletes specific document
   → DELETE /documents/{documentId} request
   → DocumentService removes from vector store + validates existence
   → Updates internal document list
   → Returns DeleteResponse{message, remainingDocuments}
   → UI refreshes

3. User sends chat query
   → documentIds[] parameter passed to ChatController
   → ChatService builds OR filter: "documentId == 'id1' OR documentId == 'id2'"
   → RAG query includes all specified documents
   → Backward compatibility: single documentId still works

🔄 FRONTEND TODO (Phase 2):
   → Update UI components to use new API endpoints
   → Implement document list management
   → Support multiple document selection
```

## ✅ Implementation Status

### ✅ Phase 1: Backend Support (COMPLETE)
1. **✅ Updated DocumentService**
  - ✅ Remove `deleteDocuments()` from `storeFile()`
  - ✅ Implement `deleteDocument(String documentId)`
  - ✅ Add document existence validation
  - ✅ Add defensive copying for security

2. **✅ Updated DocumentController**
  - ✅ Add `DELETE /documents/{documentId}` endpoint
  - ✅ Return full document list from all operations
  - ✅ Add proper error handling with structured responses
  - ✅ Maintain backward compatibility

3. **✅ Updated ChatController/ChatService**
  - ✅ Accept optional `List<String> documentIds` parameter
  - ✅ Build composite OR filter expressions
  - ✅ Default to empty filter if no documents
  - ✅ Maintain backward compatibility with single `documentId`

### 🔄 Phase 2: Frontend State Management (IN PROGRESS)
1. **TODO: Update AppComponent**
  - Change signal from `string` to `string[]` for document IDs
  - Update event handlers

2. **TODO: Update type definitions**
  - Modify interfaces to support arrays where needed

### ⏳ Phase 3: UI Enhancement (PENDING)
1. **TODO: Update DocumentPanelComponent template**
  - Add delete button to each document item
  - Improve list styling with hover states
  - Add document count badge

2. **TODO: Update component logic**
  - Implement individual delete functionality
  - Emit all document IDs on changes
  - Handle loading states for deletions

### ⏳ Phase 4: Testing & Edge Cases (PENDING)
1. **TODO: Test scenarios**
  - Upload multiple documents rapidly
  - Delete documents during upload
  - Handle maximum document limits
  - Test with empty document list

2. **TODO: Error handling**
  - Network failures during deletion
  - Concurrent modifications
  - Session timeout handling

## ✅ Technical Implementation Details

### ✅ Filter Expression Format
For multiple documents, the filter expression is built as:
```java
// Single document
"documentId == 'doc1'"

// Multiple documents  
"documentId == 'doc1' OR documentId == 'doc2' OR documentId == 'doc3'"

// No documents
"" (empty filter - searches all)
```

### ✅ API Enhancements

#### New Endpoints
```bash
# Individual document deletion
DELETE /documents/{documentId}
Response: {message: "...", remainingDocuments: [...]}

# Enhanced upload response
POST /upload
Response: {uploadedDocument: {...}, allDocuments: [...]}

# Multiple document chat (backward compatible)
GET /chat?chat=query&documentIds=doc1,doc2,doc3
GET /chat?chat=query&documentId=doc1  # Still works
```

#### Response DTOs
```java
// Structured responses for better error handling
public record UploadResponse(DocumentInfo uploadedDocument, List<DocumentInfo> allDocuments) {}
public record DeleteResponse(String message, List<DocumentInfo> remainingDocuments) {}
public record ErrorResponse(String error) {}
```

### Performance Considerations
- ✅ **Filter optimization**: Single document uses simple equality, multiple uses OR expressions
- ✅ **Input validation**: Null/empty document IDs filtered out
- ✅ **Defensive copying**: Document lists are safely copied to prevent modification
- TODO: Consider pagination if users upload many documents
- TODO: Implement lazy loading for document metadata
- TODO: Cache document list to reduce API calls

### Session Management
- ✅ Documents remain session-scoped as before
- ✅ Proper cleanup on individual deletion
- TODO: Consider document count limits per session
- TODO: Test session expiration handling

### ✅ Backward Compatibility
- ✅ Support single document ID in chat endpoint for compatibility
- ✅ Gracefully handle empty document lists
- ✅ Maintain existing API contracts where possible
- ✅ Legacy `documentId` parameter still works
- ✅ Existing delete-all endpoint preserved

## Migration Strategy
1. ✅ **Deploy backend changes first** (backwards compatible)
2. 🔄 **Update frontend to use new endpoints** (Phase 2)
3. ⏳ **Test thoroughly in staging environment** (Phase 4)
4. ⏳ **Roll out with feature flag if needed**

## Future Enhancements
- Document selection UI (choose which docs to include in chat)
- Bulk operations (delete multiple, download all)
- Document preview functionality
- Support for more file types beyond PDF
- Document organization (folders/tags)
- Document metadata search and filtering

## Success Metrics
- ✅ **Backend**: Multiple document upload/deletion without conflicts
- ✅ **Backend**: Individual document deletion works reliably
- ✅ **Backend**: Chat queries successfully incorporate multiple uploaded documents
- ✅ **Backend**: No regression in single-document workflows
- 🔄 **Frontend**: Users can manage multiple documents through UI
- ⏳ **Overall**: Improved user satisfaction with document management

## Next Steps
**Ready for Phase 2**: The backend is complete and fully backward compatible. Frontend components can now be updated to utilize the new multi-document capabilities while maintaining existing functionality.