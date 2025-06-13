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
- **✅ Completed**: Gracefully handles servers that don't support prompts (tools-only servers)
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
- **✅ Completed**: Fixed CORS configuration conflicts

#### 4. Supporting Infrastructure
- **✅ Completed**: MCP Client Factory for consistent client configuration
- **✅ Completed**: Updated Metrics Service with prompt information
- **✅ Completed**: Comprehensive error handling and validation
- **✅ Completed**: Unit tests for core functionality
- **✅ Completed**: Graceful handling of MCP servers without prompts support

### Frontend Components (✅ Implemented)

#### 1. Prompts Panel
- **✅ Completed**: New side panel following existing component patterns
- **✅ Completed**: Lists all available prompts grouped by MCP server using expansion panels
- **✅ Completed**: Shows prompt descriptions and required arguments with visual chips
- **✅ Completed**: Displays prompt availability status with consistent styling
- **✅ Completed**: Integrated with existing sidenav service for coordinated behavior
- **✅ Completed**: Handles loading, error, and empty states gracefully
- **✅ Completed**: Badge on toggle button showing total prompt count
- **✅ Completed**: Refresh functionality to reload prompts

#### 2. Angular Prompt Service
- **✅ Completed**: Service to fetch and cache prompts from backend APIs
- **✅ Completed**: Handles all prompt-related HTTP requests with proper error handling
- **✅ Completed**: TypeScript interfaces matching backend models
- **✅ Completed**: Integrated with existing HTTP client setup and URL resolution

#### 3. Platform Metrics Integration
- **✅ Completed**: Extended `PlatformMetrics` interface to include prompt information
- **✅ Completed**: Updated metrics polling to include prompt availability status
- **✅ Completed**: Display prompt status in the prompts panel UI

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

### Frontend Models (✅ Implemented)

```typescript
interface McpPrompt {
    serverId: string;
    name: string;
    description: string;
    arguments: PromptArgument[];
    id?: string;  // computed: serverId:name
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

interface PromptMetrics {
    totalPrompts: number;
    serversWithPrompts: number;
    available: boolean;
}

// Extended PlatformMetrics interface
interface PlatformMetrics {
    conversationId: string;
    chatModel: string;
    embeddingModel: string;
    vectorStoreName: string;
    agents: Agent[];
    prompts: PromptMetrics;  // ✅ Added
}
```

## User Experience Flows

### 1. Discovery and Browsing Flow
```
App startup → Backend discovers prompts → Prompts panel populated
User opens prompts panel → Browse by server → Filter/search prompts
```

### 2. Prompt Selection Flow (📋 Ready for Implementation)
```
User clicks prompt button in chat → Prompt selection dialog opens
Browse/search prompts → Select prompt → View argument form (if needed)
```

### 3. Argument Configuration Flow (📋 Future)
```
Selected prompt has arguments → Dynamic form generated → User fills arguments
Real-time validation → Preview resolved prompt → Confirm or edit
```

### 4. Execution Flow (📋 Future)
```
Prompt resolved → Content inserted into chat → User can edit before sending
Send message → Standard chat flow with LLM response
```

## UI Design Implementation

### Prompt Access Methods
1. **✅ Dedicated Panel**: Side panel for browsing all prompts with expansion groups by server
2. **📋 Quick Access Button**: Prompt button next to chat input (future)
3. **📋 Slash Commands**: Type `/prompt-name` to trigger prompt (future)
4. **📋 Context Menu**: Right-click integration (future)
5. **📋 Command Palette**: Searchable command interface (future)

### Implemented UI Features
- **✅ Prompt Categories**: Prompts grouped by MCP server
- **✅ Status Indicators**: Visual status for prompt availability
- **✅ Argument Preview**: Required/optional argument counts with color-coded chips
- **✅ Server Health**: Integration with existing agent health checking
- **✅ Responsive Design**: Follows established component patterns and theming

## Implementation Status

### ✅ Phase 1: Backend Foundation (Completed)
1. **✅ Task 1.1**: Extended MCP client to discover prompts
- Modified `ChatConfiguration` to call `prompts/list`
- Store prompts in `PromptDiscoveryService`
- Handle servers without prompts support gracefully

2. **✅ Task 1.2**: Created Prompt Services
- `PromptDiscoveryService` for storage and retrieval
- `PromptResolutionService` for resolution logic
- Multi-server prompt management with namespacing
- `McpClientFactory` for consistent client creation

3. **✅ Task 1.3**: Added REST endpoints
- `PromptController` with comprehensive API
- Argument validation and error handling
- Updated metrics to include prompt information
- Fixed CORS configuration conflicts

### ✅ Phase 2: Frontend Infrastructure (Completed)
4. **✅ Task 2.1**: Created Prompts Panel Component
- New Angular component in `src/main/frontend/src/prompts-panel/`
- Full component implementation with TypeScript, HTML, CSS, and spec files
- Integrated with existing sidenav service
- Added toggle button positioned after memory panel

5. **✅ Task 2.2**: Implemented Angular Prompt Service
- Created `PromptService` in `src/main/frontend/src/services/`
- Service to fetch and cache prompts with proper error handling
- TypeScript interfaces matching backend models
- Integrated with existing HTTP client setup

6. **✅ Task 2.3**: Updated Platform Metrics UI
- Extended `PlatformMetrics` interface to include `PromptMetrics`
- Updated metrics polling to include prompts data
- Display prompt availability status in UI
- Updated `AppComponent` to include new prompts panel

### 📋 Phase 3: UI Components (Next Phase)
7. **Task 3.1**: Create Prompt Selection UI
- Design prompt selection dialog/dropdown
- Implement search and filtering
- Quick access from chat interface

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

## Implementation Notes and Lessons Learned

### Issues Encountered and Resolved

1. **MCP Servers Without Prompts Support**
  - **Issue**: Some MCP servers (like bitcoin-mcp-server) only provide tools, not prompts
  - **Error**: `Method not found: prompts/list` causing application startup failure
  - **Solution**: Modified `PromptDiscoveryService` to catch `McpError` and gracefully handle missing prompts support
  - **Result**: Application starts successfully with mixed tool-only and prompt-enabled servers

2. **CORS Configuration Conflict**
  - **Issue**: Added `@CrossOrigin(origins = "*")` to `PromptController` conflicted with global CORS config
  - **Error**: "When allowCredentials is true, allowedOrigins cannot contain '*'"
  - **Solution**: Removed controller-level CORS annotation, relying on global `WebConfiguration`
  - **Result**: CORS works correctly for all endpoints including new prompt APIs

3. **TypeScript Strict Null Checking**
  - **Issue**: Optional chaining in template comparisons flagged by TypeScript compiler
  - **Error**: "Object is possibly 'undefined'" in Angular template
  - **Solution**: Used explicit null checking with `&&` instead of optional chaining in comparisons
  - **Result**: Clean TypeScript compilation with proper null safety

### Directory Structure Implemented

```
src/main/frontend/src/
├── services/
│   └── prompt.service.ts              # ✅ NEW - Angular service for prompt APIs
├── prompts-panel/                     # ✅ NEW - Complete component directory
│   ├── prompts-panel.component.ts     # ✅ NEW - Component logic
│   ├── prompts-panel.component.html   # ✅ NEW - Component template  
│   ├── prompts-panel.component.css    # ✅ NEW - Component styles
│   └── prompts-panel.component.spec.ts # ✅ NEW - Unit tests
├── app/
│   ├── app.component.ts               # ✅ UPDATED - Added prompts support
│   └── app.component.html             # ✅ UPDATED - Added prompts panel
```

## Technical Considerations

### Performance
- **✅ Implemented**: Cache discovered prompts to avoid repeated calls
- **✅ Implemented**: Efficient in-memory storage and lookup
- **✅ Implemented**: Graceful error handling for server communication failures
- **📋 Future**: Lazy load prompt details only when needed
- **📋 Future**: Debounce search/filter operations

### Error Handling
- **✅ Implemented**: Graceful degradation if prompt discovery fails
- **✅ Implemented**: Clear error messages for validation failures
- **✅ Implemented**: Comprehensive exception handling
- **✅ Implemented**: Handle servers without prompts support
- **📋 Future**: Fallback to regular chat if prompts unavailable

### Security
- **✅ Implemented**: Validate all prompt arguments server-side
- **✅ Implemented**: Sanitize resolved prompt content
- **✅ Implemented**: Proper CORS configuration for credential support
- **📋 Future**: Implement rate limiting for prompt resolution
- **📋 Future**: Access controls and audit logging
- **📋 Future**: Protection against prompt injection attacks

### Extensibility
- **✅ Implemented**: Modular service architecture
- **✅ Implemented**: Support for multiple content types
- **✅ Implemented**: Consistent component patterns following existing architecture
- **📋 Future**: Plugin system for custom prompt sources
- **📋 Future**: Prompt versioning and migration support
- **📋 Future**: Custom argument types and validators

## Success Metrics

1. **✅ Discoverability**: Backend API provides comprehensive prompt discovery with graceful fallbacks
2. **✅ Performance**: Prompt discovery doesn't impact application startup significantly
3. **✅ Robustness**: Application handles mixed MCP server environments (tools-only + prompts)
4. **✅ Integration**: Prompts panel integrates seamlessly with existing UI patterns
5. **📋 Usability**: Argument forms will be intuitive with helpful validation (future)
6. **📋 Adoption**: Users will prefer prompts for common tasks over free-form input (future)

## MCP Specification Compliance

### ✅ Implemented Features
- ✅ Prompt discovery via `prompts/list`
- ✅ Prompt resolution via `prompts/get`
- ✅ Multi-server support with namespacing
- ✅ Argument validation and handling
- ✅ Multiple content type support (text, image, resource)
- ✅ Error handling and graceful degradation
- ✅ Backward compatibility with tools-only servers

### 📋 Specification Features for Future Implementation
- 📋 Embedded resource context expansion
- 📋 Multi-step workflow support
- 📋 Real-time prompt change notifications
- 📋 Advanced argument schema validation
- 📋 Prompt composition and chaining
- 📋 Rate limiting and access controls

## Next Steps

### Immediate Next Steps (Phase 3)
1. **Implement Prompt Selection Dialog**: Create a modal/dialog for selecting prompts from the chat interface
2. **Add Chat Integration**: Connect prompt selection to the chat input system
3. **Build Argument Forms**: Create dynamic forms for prompts that require arguments

### Future Enhancements (Phase 4+)
1. **Advanced UI Features**: Slash commands, favorites, recent prompts
2. **Rich Content Support**: Image and resource handling in prompts
3. **Real-time Updates**: Prompt change notifications and auto-refresh
4. **Enhanced Error Handling**: Better user feedback and recovery options

## Conclusion

**Phase 1 (Backend Foundation) and Phase 2 (Frontend Infrastructure) are now complete and production-ready.** The implementation successfully handles real-world scenarios including:

- Mixed MCP server environments (some with prompts, some tools-only)
- Proper CORS configuration for credential-based sessions
- Robust error handling and graceful degradation
- Clean integration with existing application architecture

The foundation is solid for implementing the remaining phases, with the prompts panel already providing value by displaying available prompts and their requirements. The next phase will focus on making prompts actionable through chat integration and argument collection forms.

This implementation demonstrates the power of Cloud Foundry's service marketplace approach - adding sophisticated AI capabilities through simple service bindings while maintaining clean separation of concerns and robust error handling.