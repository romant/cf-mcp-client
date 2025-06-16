package org.tanzu.mcpclient.prompt;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents an argument that a prompt can accept.
 * Contains metadata about the argument including validation rules and default values.
 *
 * @param name The name of the argument
 * @param description Human-readable description of the argument
 * @param required Whether this argument must be provided
 * @param defaultValue Default value for the argument (if any)
 * @param schema JSON schema for validation (if any)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PromptArgument(
        @JsonProperty("name") String name,
        @JsonProperty("description") String description,
        @JsonProperty("required") boolean required,
        @JsonProperty("defaultValue") Object defaultValue,
        @JsonProperty("schema") Object schema
) {

    /**
     * Returns true if this argument has a default value.
     */
    public boolean hasDefaultValue() {
        return defaultValue != null;
    }

    /**
     * Returns true if this argument has a validation schema.
     */
    public boolean hasSchema() {
        return schema != null;
    }
}
