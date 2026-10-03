package com.springai.jdd.assistant.agent.refusal;

import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static lombok.AccessLevel.PRIVATE;
import static org.springframework.util.StringUtils.hasText;

/**
 * Reads a refusal that a model wrote as prose instead of calling the refusal tool, so a weaker
 * model still yields a typed reason rather than an unstructured apology.
 */
@NoArgsConstructor(access = PRIVATE)
final class RefusalText {

    private static final Pattern WRITTEN_CALL = Pattern.compile(RefusalDescriptors.NAME + "\\s*\\(\\s*\"?(\\w+)\"?\\s*\\)");
    private static final int REASON = 1;

    static RefusalReason detect(String content) {
        if (!hasText(content)) {
            return null;
        }
        Matcher written = WRITTEN_CALL.matcher(content);
        return written.find() ? resolve(written.group(REASON)) : null;
    }

    private static RefusalReason resolve(String name) {
        return Arrays.stream(RefusalReason.values())
                     .filter(reason -> reason.name().equals(name))
                     .findFirst()
                     .orElse(null);
    }
}
