package com.springai.jdd.assistant.api;

import com.springai.jdd.assistant.agent.QueryOrchestrator;
import com.springai.jdd.assistant.api.exception.InvalidQueryException;
import com.springai.jdd.assistant.api.dto.QueryRequest;
import com.springai.jdd.assistant.api.dto.QueryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static lombok.AccessLevel.PACKAGE;
import static org.springframework.util.StringUtils.hasText;

@RestController
@EnableConfigurationProperties(QueryProperties.class)
@RequiredArgsConstructor(access = PACKAGE)
class QueryController {

    private static final String EMPTY_PROMPT_MESSAGE = "A prompt is required.";
    private static final String LONG_PROMPT_MESSAGE = "A prompt may be at most %d characters.";

    private final QueryOrchestrator orchestrator;
    private final QueryProperties properties;

    @PostMapping(ApiPaths.QUERY)
    QueryResponse answerQuery(@RequestBody QueryRequest request) {
        requirePrompt(request);
        return QueryResponseFactory.buildFrom(orchestrator.answer(request.prompt(), request.conversationId()));
    }

    private void requirePrompt(QueryRequest request) {
        if (request == null || !hasText(request.prompt())) {
            throw new InvalidQueryException(EMPTY_PROMPT_MESSAGE);
        }
        if (request.prompt().length() > properties.maxPromptLength()) {
            throw new InvalidQueryException(LONG_PROMPT_MESSAGE.formatted(properties.maxPromptLength()));
        }
    }
}
