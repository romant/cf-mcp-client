# Multiple Document Support - Design & Implementation Plan

## Overview
This document outlines the design and implementation plan for expanding the document management functionality to support multiple document uploads with individual document management capabilities.

## Current State Analysis

### Limitations
1. **Single Document Constraint**: `DocumentService.storeFile()` deletes all documents before adding a new one
2. **Single Document ID Tracking**: The application tracks only one `currentDocumentId`
3. **No Individual Delete**: Users can only clear all documents at once
4. **Simple Filter Expression**: Chat queries filter by a single document ID

### Existing Infrastructure
- Unique document ID generation per upload
- Vector store supports multiple documents with metadata
- Backend maintains a `documentList` collection
- Filter expression infrastructure exists in `ChatService`

## Proposed Design

### Core Requirements
1. Support uploading multiple documents without removing existing ones
2. Display all uploaded documents with metadata (name, size, upload date)
3. Allow individual document deletion
4. Include all uploaded documents in RAG queries by default
5. Maintain session-based document persistence

### Architecture Changes

#### Backend Changes

##### DocumentService.java
- **Remove auto-delete**: Eliminate `deleteDocuments()` call in `storeFile()`
- **Add method**: `deleteDocument(String documentId)` for individual deletion
- **Enhance**: Return updated document list after each operation

##### DocumentController.java
- **New endpoint**: `DELETE /documents/{documentId}` for individual document deletion
- **Modify**: Upload endpoint to return complete document list
- **Enhance**: Add validation for document existence

##### ChatService.java
- **Multiple ID support**: Accept `List<String> documentIds` instead of single ID
- **Filter expression**: Build OR condition for multiple document IDs
- **Default behavior**: Use all documents if no specific selection

#### Frontend Changes

##### DocumentPanelComponent
- **UI Updates**:
  - Show scrollable list of all documents
  - Add delete button for each document
  - Show document count in panel
  - Improve empty state messaging
- **State Management**:
  - Track all uploaded documents
  - Handle individual deletion
  - Emit all document IDs to parent

##### AppComponent
- **State change**: Replace `currentDocumentId` with `currentDocumentIds: string[]`
- **Update**: Document selection handler to manage arrays

##### ChatboxComponent
- **Props update**: Accept `documentIds: string[]` instead of single `documentId`
- **Query params**: Send multiple document IDs to chat endpoint

### Data Flow

```
1. User uploads document
   → DocumentController generates unique ID
   → DocumentService adds to vector store (no deletion)
   → Returns updated document list
   → UI updates to show all documents

2. User deletes specific document
   → DELETE request with document ID
   → DocumentService removes from vector store
   → Updates internal document list
   → Returns updated list
   → UI refreshes

3. User sends chat query
   → All document IDs passed to ChatService
   → Filter expression: "documentId == 'id1' OR documentId == 'id2' OR ..."
   → RAG query includes all documents
```

## Implementation Plan

### Phase 1: Backend Support (Priority: High)
1. **Update DocumentService**
   - Remove `deleteDocuments()` from `storeFile()`
   - Implement `deleteDocument(String documentId)`
   - Add document existence validation

2. **Update DocumentController**
   - Add `DELETE /documents/{documentId}` endpoint
   - Return full document list from all operations
   - Add proper error handling

3. **Update ChatController/ChatService**
   - Accept optional `List<String> documentIds` parameter
   - Build composite filter expressions
   - Default to empty filter if no documents

### Phase 2: Frontend State Management (Priority: High)
1. **Update AppComponent**
   - Change signal from `string` to `string[]` for document IDs
   - Update event handlers

2. **Update type definitions**
   - Modify interfaces to support arrays where needed

### Phase 3: UI Enhancement (Priority: Medium)
1. **Update DocumentPanelComponent template**
   - Add delete button to each document item
   - Improve list styling with hover states
   - Add document count badge

2. **Update component logic**
   - Implement individual delete functionality
   - Emit all document IDs on changes
   - Handle loading states for deletions

### Phase 4: Testing & Edge Cases (Priority: Medium)
1. **Test scenarios**
   - Upload multiple documents rapidly
   - Delete documents during upload
   - Handle maximum document limits
   - Test with empty document list

2. **Error handling**
   - Network failures during deletion
   - Concurrent modifications
   - Session timeout handling

## Technical Considerations

### Filter Expression Format
For multiple documents, the filter expression should be:
```
documentId == 'doc1' OR documentId == 'doc2' OR documentId == 'doc3'
```

Spring AI's vector store should handle this OR expression properly.

### Performance
- Consider pagination if users upload many documents
- Implement lazy loading for document metadata
- Cache document list to reduce API calls

### Session Management
- Documents are already session-scoped
- Ensure proper cleanup on session expiration
- Consider document count limits per session

### Backward Compatibility
- Support single document ID in chat endpoint for compatibility
- Gracefully handle empty document lists
- Maintain existing API contracts where possible

## Migration Strategy
1. Deploy backend changes first (backwards compatible)
2. Update frontend to use new endpoints
3. Test thoroughly in staging environment
4. Roll out with feature flag if needed

## Future Enhancements
- Document selection UI (choose which docs to include in chat)
- Bulk operations (delete multiple, download all)
- Document preview functionality
- Support for more file types beyond PDF
- Document organization (folders/tags)

## Success Metrics
- Users can upload multiple documents without losing previous ones
- Individual document deletion works reliably
- Chat queries successfully incorporate all uploaded documents
- No regression in single-document workflows
- Improved user satisfaction with document management