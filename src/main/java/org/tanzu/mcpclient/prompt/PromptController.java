package org.tanzu.mcpclient.prompt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller providing endpoints for MCP prompt discovery and resolution.
 * This controller handles HTTP requests for listing available prompts and
 * resolving prompts with user-provided arguments.
 *
 * <p>Endpoints provided:</p>
 * <ul>
 *   <li>GET /prompts - List all available prompts</li>
 *   <li>GET /prompts/servers/{serverId} - List prompts from a specific server</li>
 *   <li>POST /prompts/resolve - Resolve a prompt with arguments</li>
 * </ul>
 *
 * @author AI Assistant
 */
@RestController
@RequestMapping("/prompts")
@CrossOrigin(origins = "*")
public class PromptController {

    private static final Logger logger = LoggerFactory.getLogger(PromptController.class);

    private final PromptDiscoveryService promptDiscoveryService;
    private final PromptResolutionService promptResolutionService;

    public PromptController(PromptDiscoveryService promptDiscoveryService,
                            PromptResolutionService promptResolutionService) {
        this.promptDiscoveryService = promptDiscoveryService;
        this.promptResolutionService = promptResolutionService;
    }

    /**
     * Lists all available prompts from all connected MCP servers.
     *
     * @return List of all discovered prompts
     */
    @GetMapping
    public ResponseEntity<List<McpPrompt>> listAllPrompts() {
        try {
            List<McpPrompt> prompts = promptDiscoveryService.getAllPrompts();
            logger.debug("Retrieved {} prompts from all servers", prompts.size());
            return ResponseEntity.ok(prompts);
        } catch (Exception e) {
            logger.error("Error retrieving prompts: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Lists prompts grouped by server.
     *
     * @return Map of server IDs to their respective prompts
     */
    @GetMapping("/by-server")
    public ResponseEntity<Map<String, List<McpPrompt>>> listPromptsByServer() {
        try {
            Map<String, List<McpPrompt>> promptsByServer = promptDiscoveryService.getPromptsByServer();
            logger.debug("Retrieved prompts from {} servers", promptsByServer.size());
            return ResponseEntity.ok(promptsByServer);
        } catch (Exception e) {
            logger.error("Error retrieving prompts by server: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Lists prompts from a specific server.
     *
     * @param serverId The ID of the server to get prompts from
     * @return List of prompts from the specified server
     */
    @GetMapping("/servers/{serverId}")
    public ResponseEntity<List<McpPrompt>> listPromptsByServerId(@PathVariable String serverId) {
        try {
            List<McpPrompt> prompts = promptDiscoveryService.findPromptsByServer(serverId);
            logger.debug("Retrieved {} prompts from server {}", prompts.size(), serverId);
            return ResponseEntity.ok(prompts);
        } catch (Exception e) {
            logger.error("Error retrieving prompts for server {}: {}", serverId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Gets details for a specific prompt by its ID.
     *
     * @param promptId The unique prompt ID (serverId:promptName)
     * @return The prompt details if found
     */
    @GetMapping("/{promptId}")
    public ResponseEntity<McpPrompt> getPromptById(@PathVariable String promptId) {
        try {
            // Handle URL encoding of prompt ID (serverId:promptName might be encoded)
            String decodedPromptId = java.net.URLDecoder.decode(promptId, "UTF-8");

            Optional<McpPrompt> prompt = promptDiscoveryService.findPromptById(decodedPromptId);
            if (prompt.isPresent()) {
                logger.debug("Retrieved prompt: {}", decodedPromptId);
                return ResponseEntity.ok(prompt.get());
            } else {
                logger.debug("Prompt not found: {}", decodedPromptId);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error retrieving prompt {}: {}", promptId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Resolves a prompt with the provided arguments.
     *
     * @param request The prompt resolution request containing prompt ID and arguments
     * @return The resolved prompt content
     */
    @PostMapping("/resolve")
    public ResponseEntity<ResolvedPrompt> resolvePrompt(@RequestBody PromptResolutionRequest request) {
        try {
            logger.info("Resolving prompt: {} with {} arguments",
                    request.promptId(),
                    request.arguments() != null ? request.arguments().size() : 0);

            ResolvedPrompt resolvedPrompt = promptResolutionService.resolvePrompt(request);

            logger.debug("Successfully resolved prompt: {}", request.promptId());
            return ResponseEntity.ok(resolvedPrompt);

        } catch (PromptResolutionService.PromptResolutionException e) {
            logger.warn("Prompt resolution failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error resolving prompt {}: {}", request.promptId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Provides status information about prompt discovery.
     *
     * @return Status information including prompt and server counts
     */
    @GetMapping("/status")
    public ResponseEntity<PromptStatus> getPromptStatus() {
        try {
            PromptStatus status = new PromptStatus(
                    promptDiscoveryService.getPromptCount(),
                    promptDiscoveryService.getServerCount(),
                    promptDiscoveryService.hasPrompts()
            );

            logger.debug("Prompt status: {} prompts from {} servers",
                    status.promptCount(), status.serverCount());
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("Error retrieving prompt status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Record representing the status of prompt discovery.
     */
    public record PromptStatus(
            int promptCount,
            int serverCount,
            boolean available
    ) {}

    /**
     * Exception handler for handling prompt-related errors.
     */
    @ExceptionHandler(PromptResolutionService.PromptResolutionException.class)
    public ResponseEntity<ErrorResponse> handlePromptResolutionException(
            PromptResolutionService.PromptResolutionException e) {
        logger.warn("Prompt resolution error: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("PROMPT_RESOLUTION_ERROR", e.getMessage()));
    }

    /**
     * General exception handler for unexpected errors.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception e) {
        logger.error("Unexpected error in prompt controller: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
    }

    /**
     * Record representing an error response.
     */
    public record ErrorResponse(
            String code,
            String message
    ) {}
}