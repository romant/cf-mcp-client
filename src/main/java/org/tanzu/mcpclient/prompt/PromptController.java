package org.tanzu.mcpclient.prompt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for prompt-related operations.
 * Provides endpoints for resolving prompts with user-provided arguments.
 */
@RestController
@RequestMapping("/prompts")
public class PromptController {

    private static final Logger logger = LoggerFactory.getLogger(PromptController.class);

    private final PromptResolutionService promptResolutionService;

    public PromptController(PromptResolutionService promptResolutionService) {
        this.promptResolutionService = promptResolutionService;
    }

    /**
     * Resolve a prompt with the provided arguments.
     *
     * @param request The prompt resolution request containing promptId and arguments
     * @return The resolved prompt content
     */
    @PostMapping("/resolve")
    public ResponseEntity<ResolvedPrompt> resolvePrompt(@RequestBody PromptResolutionRequest request) {
        logger.info("Resolving prompt: {} with {} arguments",
                request.promptId(),
                request.arguments() != null ? request.arguments().size() : 0);

        try {
            ResolvedPrompt resolvedPrompt = promptResolutionService.resolvePrompt(request);
            logger.debug("Successfully resolved prompt: {}", request.promptId());
            return ResponseEntity.ok(resolvedPrompt);

        } catch (PromptResolutionService.PromptResolutionException e) {
            logger.error("Failed to resolve prompt {}: {}", request.promptId(), e.getMessage());
            return ResponseEntity.badRequest().build();

        } catch (Exception e) {
            logger.error("Unexpected error resolving prompt {}: {}", request.promptId(), e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}