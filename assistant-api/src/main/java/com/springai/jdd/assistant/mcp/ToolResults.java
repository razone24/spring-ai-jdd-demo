package com.springai.jdd.assistant.agent.tool;

import lombok.NoArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;

import static lombok.AccessLevel.PRIVATE;

/**
 * Turns raw tool input/output into something a person can read in the UI and the audit trail.
 */
@NoArgsConstructor(access = PRIVATE)
final class ToolResults {

    static final int PREVIEW_LIMIT = 1500;
    private static final String ELLIPSIS = "…";
    private static final int MAX_ENCODING_LAYERS = 3;
    private static final Set<String> ENVELOPE_FIELDS = Set.of("type", "text", "annotations", "meta", "_meta");

    static Object argumentsOf(String input, ObjectMapper objectMapper) {
        if (input == null || input.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(input, Object.class);
        } catch (JacksonException exception) {
            return input;
        }
    }

    /**
     * MCP tool callbacks return the tool's text JSON-encoded — a quoted string, or the content list
     * ({@code [{"type":"text","text":"..."}]}); the preview shows the text inside, not the envelope.
     */
    static String previewOf(String output, ObjectMapper objectMapper) {
        String text = output;
        // The envelope can hold a tool result that the server JSON-encoded once more (a String return value).
        for (int layer = 0; layer < MAX_ENCODING_LAYERS; layer++) {
            String unwrapped = unwrapText(text, objectMapper);
            if (unwrapped == null || unwrapped.equals(text)) {
                break;
            }
            text = unwrapped;
        }
        return truncate(text);
    }

    static String truncate(String text) {
        if (text == null || text.length() <= PREVIEW_LIMIT) {
            return text;
        }
        return text.substring(0, PREVIEW_LIMIT) + ELLIPSIS;
    }

    private static boolean isTextContent(JsonNode item) {
        if (!item.has("text")) {
            return false;
        }
        for (String field : item.propertyNames()) {
            if (!ENVELOPE_FIELDS.contains(field)) {
                return false;
            }
        }
        return true;
    }

    private static String unwrapText(String output, ObjectMapper objectMapper) {
        if (output == null || !(output.startsWith("[") || output.startsWith("\""))) {
            return output;
        }
        try {
            JsonNode content = objectMapper.readTree(output);
            if (content.isString()) {
                return content.asString();
            }
            StringBuilder text = new StringBuilder();
            for (JsonNode item : content) {
                if (!isTextContent(item)) {
                    return output;
                }
                text.append(item.path("text").asString());
            }
            return text.isEmpty() ? output : text.toString();
        } catch (JacksonException exception) {
            return output;
        }
    }
}
