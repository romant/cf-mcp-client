# MCP Prompts Integration Design

## Overview

This document outlines the design and implementation for adding Model Context Protocol (MCP) prompts support to the Tanzu Platform Chat application. MCP prompts are reusable templates and workflows that servers can offer to standardize and streamline common LLM interactions. They provide user-controlled, predefined message templates that can accept dynamic arguments, include context from resources, and guide specific workflows.

## MCP Prompts Specification Summary

Based on the official MCP specification, prompts provide the following capabilities:

- **Dynamic Arguments**: Accept user-provided parameters for customization
- **Embedded Resource Context**: Include content from MCP resources (files, databases, APIs)
- **Multi-step Workflows**: Chain multiple interactions and guide complex workflows
- **Rich Content Types**: Support text, images, and embedded resources
- **Real-time Updates**: Notify clients when prompt templates change
- **UI Integration**: Surface as slash commands, quick actions, and interactive forms

## Current State Analysis

The application currently:
- Discovers MCP tools via `tools/list` at startup
- Displays available tools in the Agents panel
- Accepts free-form text input in the chat interface
- Sends chat messages to the `/chat` endpoint

## Design Goals

1. **Discover Available Prompts**: Automatically discover prompts from all connected MCP servers at startup
2. **Intuitive UI**: Provide an easy way for users to browse and select prompts
3. **Dynamic Forms**: Generate forms for prompt arguments based on their schemas
4. **Seamless Integration**: Integrate prompts naturally into the existing chat flow
5. **Multi-Server Support**: Handle prompts from multiple MCP servers without conflicts
6. **Rich Content Support**: Handle text, image, and embedded resource content types
7. **Real-time Updates**: Support dynamic prompt updates and notifications

## Architecture Design

### Backend Components (✅ Implemented)

#### 1. Prompt Discovery Service
- **✅ Completed**: Extends MCP client initialization to call `prompts/list` alongside `tools/list`
- **✅ Completed**: Stores discovered prompts with metadata (name, description, arguments)
- **✅ Completed**: Handles prompt namespacing to avoid conflicts between servers
- **🔄 Future Enhancement**: Support for prompt change notifications via `notifications/prompts/list_changed`

#### 2. Prompt Resolution Service
- **✅ Completed**: Implements `prompts/get` calls with user-provided arguments
- **✅ Completed**: Validates arguments against prompt requirements
- **✅ Completed**: Handles multiple content types (TextContent, ImageContent, EmbeddedResource)
- **✅ Completed**: Transforms resolved prompts into chat messages
- **🔄 Future Enhancement**: Support for embedded resource context expansion

#### 3. REST API Extensions
- **✅ Completed**: `GET /prompts` - List all available prompts from all MCP servers
- **✅ Completed**: `GET /prompts/by-server` - Group prompts by server
- **✅ Completed**: `GET /prompts/servers/{serverId}` - Server-specific prompts
- **✅ Completed**: `GET /prompts/{promptId}` - Individual prompt details
- **✅ Completed**: `POST /prompts/resolve` - Resolve prompts with arguments
- **✅ Completed**: `GET /prompts/status` - System status and metrics
- **✅ Completed**: Extended `/metrics` to include prompt availability

#### 4. Supporting Infrastructure
- **✅ Completed**: MCP Client Factory for consistent client configuration
- **✅ Completed**: Updated Metrics Service with prompt information
- **✅ Completed**: Comprehensive error handling and validation
- **✅ Completed**: Unit tests for core functionality

### Frontend Components (📋 Design Phase)

#### 1. Prompts Panel
- New side panel similar to existing panels (Chat, Documents, Agents, Memory)
- Lists all available prompts grouped by MCP server
- Shows prompt descriptions and required arguments
- **Enhanced**: Displays argument schemas and validation rules
- **Enhanced**: Shows prompt content type indicators (text, image, resource)

#### 2. Prompt Selection UI
- Quick access button in the chat input area
- Opens a dialog/dropdown showing available prompts
- Search/filter functionality for finding prompts
- **Enhanced**: Support for slash commands (e.g., `/prompt-name`)
- **Enhanced**: Context menu integration
- **Enhanced**: Command palette entries

#### 3. Argument Collection Form
- Dynamic form generation based on prompt argument schemas
- Input validation based on argument requirements
- Preview of the resolved prompt before sending
- **Enhanced**: Support for complex argument types and nested schemas
- **Enhanced**: Real-time argument validation with detailed error messages
- **Enhanced**: Argument auto-completion and suggestions

#### 4. Chat Integration
- Replace chat input with resolved prompt text
- Option to edit the resolved prompt before sending
- Clear indication when using a prompt vs free-form text
- **Enhanced**: Support for multi-message prompts
- **Enhanced**: Handle embedded resource content
- **Enhanced**: Display rich content types (images, resources)

## Data Models (✅ Implemented)

### Backend Models

```java
// Core prompt representation
public record McpPrompt(
    String serverId,
    String name,
    String description,
    List<PromptArgument> arguments
) {
    public String getId() { return serverId + ":" + name; }
    public boolean hasRequiredArguments() { /* implementation */ }
}

// Prompt argument specification
public record PromptArgument(
    String name,
    String description,
    boolean required,
    Object defaultValue,
    Object schema  // JSON Schema for validation
) {
    public boolean hasDefaultValue() { /* implementation */ }
    public boolean hasSchema() { /* implementation */ }
}

// Resolved prompt result
public record ResolvedPrompt(
    String content,
    List<PromptMessage> messages,  // Support for multi-message prompts
    Map<String, Object> metadata
) {
    public boolean hasMessages() { /* implementation */ }
    public String getPrimaryContent() { /* implementation */ }
}

// Structured message with rich content support
public record PromptMessage(
    String role,    // user, assistant, system
    String content  // Extracted from MCP Content types
) {}

// Request/Response models
public record PromptResolutionRequest(
    String promptId,
    Map<String, Object> arguments
) {}
```

### Frontend Models (📋 To Be Implemented)

```typescript
interface McpPrompt {
    serverId: string;
    name: string;
    description: string;
    arguments: PromptArgument[];
    id: string;  // computed: serverId:name
    hasRequiredArguments: boolean;
}

interface PromptArgument {
    name: string;
    description: string;
    required: boolean;
    defaultValue?: any;
    schema?: any;  // JSON Schema for UI generation
}

interface ResolvedPrompt {
    content: string;
    messages: PromptMessage[];
    metadata: Record<string, any>;
}

interface PromptMessage {
    role: 'user' | 'assistant' | 'system';
    content: string;
}

// Enhanced UI models
interface PromptFormField {
    name: string;
    type: 'text' | 'number' | 'boolean' | 'select' | 'textarea';
    label: string;
    description: string;
    required: boolean;
    defaultValue?: any;
    options?: string[];  // for select fields
    validation?: ValidationRule[];
}
```

## User Experience Flows

### 1. Discovery and Browsing Flow
```
App startup → Backend discovers prompts → Prompts panel populated
User opens prompts panel → Browse by server → Filter/search prompts
```

### 2. Prompt Selection Flow
```
User clicks prompt button in chat → Prompt selection dialog opens
Browse/search prompts → Select prompt → View argument form (if needed)
```

### 3. Argument Configuration Flow
```
Selected prompt has arguments → Dynamic form generated → User fills arguments
Real-time validation → Preview resolved prompt → Confirm or edit
```

### 4. Execution Flow
```
Prompt resolved → Content inserted into chat → User can edit before sending
Send message → Standard chat flow with LLM response
```

### 5. Advanced Workflows (🔄 Future)
```
Multi-step prompt → Execute first step → Collect intermediate results
Chain to next step → Continue until workflow complete
```

## UI Design Concepts

### Prompt Access Methods
1. **✅ Dedicated Panel**: Side panel for browsing all prompts
2. **📋 Quick Access Button**: Prompt button next to chat input
3. **📋 Slash Commands**: Type `/prompt-name` to trigger prompt
4. **📋 Context Menu**: Right-click integration
5. **📋 Command Palette**: Searchable command interface

### Enhanced UI Features
- **Prompt Categories**: Group prompts by functionality (analysis, generation, workflow)
- **Favorites**: Allow users to favorite frequently used prompts
- **Recent Prompts**: Show recently used prompts for quick access
- **Prompt Previews**: Show sample output or descriptions
- **Argument Presets**: Save common argument combinations

## Implementation Status

### ✅ Phase 1: Backend Foundation (Completed)
1. **✅ Task 1.1**: Extended MCP client to discover prompts
  - Modified `ChatConfiguration` to call `prompts/list`
  - Store prompts in `PromptDiscoveryService`

2. **✅ Task 1.2**: Created Prompt Services
  - `PromptDiscoveryService` for storage and retrieval
  - `PromptResolutionService` for resolution logic
  - Multi-server prompt management with namespacing

3. **✅ Task 1.3**: Added REST endpoints
  - `PromptController` with comprehensive API
  - Argument validation and error handling
  - Updated metrics to include prompt information

### 📋 Phase 2: Frontend Infrastructure (Next Phase)
4. **Task 2.1**: Create Prompts Panel Component
  - New Angular component for prompts side panel
  - Integrate with existing sidenav service
  - Add toggle button to toolbar

5. **Task 2.2**: Implement Angular Prompt Service
  - Service to fetch and cache prompts
  - Handle prompt resolution requests
  - Integrate with existing HTTP client setup

6. **Task 2.3**: Update Platform Metrics UI
  - Extend `PlatformMetrics` interface
  - Update metrics polling to include prompts
  - Display prompt availability status

### 📋 Phase 3: UI Components (Future)
7. **Task 3.1**: Create Prompt Selection UI
  - Design prompt selection dialog/dropdown
  - Implement search and filtering
  - Group prompts by server

8. **Task 3.2**: Build Dynamic Argument Form
  - Create form generator based on JSON schemas
  - Implement validation logic with error messages
  - Add preview functionality

9. **Task 3.3**: Integrate with Chat Interface
  - Add prompt trigger button to chat input
  - Handle prompt selection flow
  - Replace input with resolved prompt

### 📋 Phase 4: Advanced Features (Future)
10. **Task 4.1**: Enhanced UI Features
  - Implement slash command support
  - Add favorites and recent prompts
  - Context menu integration

11. **Task 4.2**: Rich Content Support
  - Handle image and resource content types
  - Implement embedded resource expansion
  - Multi-message prompt display

12. **Task 4.3**: Real-time Updates
  - Implement prompt change notifications
  - Auto-refresh prompt list
  - Handle server connection changes

## Technical Considerations

### Performance
- **✅ Implemented**: Cache discovered prompts to avoid repeated calls
- **✅ Implemented**: Efficient in-memory storage and lookup
- **📋 Future**: Lazy load prompt details only when needed
- **📋 Future**: Debounce search/filter operations

### Error Handling
- **✅ Implemented**: Graceful degradation if prompt discovery fails
- **✅ Implemented**: Clear error messages for validation failures
- **✅ Implemented**: Comprehensive exception handling
- **📋 Future**: Fallback to regular chat if prompts unavailable

### Security
- **✅ Implemented**: Validate all prompt arguments server-side
- **✅ Implemented**: Sanitize resolved prompt content
- **📋 Future**: Implement rate limiting for prompt resolution
- **📋 Future**: Access controls and audit logging
- **📋 Future**: Protection against prompt injection attacks

### Extensibility
- **✅ Implemented**: Modular service architecture
- **✅ Implemented**: Support for multiple content types
- **📋 Future**: Plugin system for custom prompt sources
- **📋 Future**: Prompt versioning and migration support
- **📋 Future**: Custom argument types and validators

## Success Metrics

1. **✅ Discoverability**: Backend API provides comprehensive prompt discovery
2. **📋 Usability**: Argument forms will be intuitive with helpful validation
3. **✅ Performance**: Prompt resolution doesn't impact chat responsiveness
4. **📋 Adoption**: Users will prefer prompts for common tasks over free-form input

## MCP Specification Compliance

### ✅ Implemented Features
- ✅ Prompt discovery via `prompts/list`
- ✅ Prompt resolution via `prompts/get`
- ✅ Multi-server support with namespacing
- ✅ Argument validation and handling
- ✅ Multiple content type support (text, image, resource)
- ✅ Error handling and graceful degradation

### 📋 Specification Features for Future Implementation
- 📋 Embedded resource context expansion
- 📋 Multi-step workflow support
- 📋 Real-time prompt change notifications
- 📋 Advanced argument schema validation
- 📋 Prompt composition and chaining
- 📋 Rate limiting and access controls

## Next Steps

1. **Begin Phase 2**: Start frontend infrastructure development
2. **Create Angular Services**: Implement prompt service for UI consumption
3. **Design UI Components**: Create mockups for prompt selection and forms
4. **Implement Basic UI**: Start with simple prompt listing and selection
5. **Iterative Enhancement**: Add advanced features based on user feedback

The backend foundation is complete and production-ready, providing a solid base for the frontend implementation and future enhancements aligned with the full MCP prompts specification.