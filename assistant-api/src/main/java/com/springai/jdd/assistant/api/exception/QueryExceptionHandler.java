package com.springai.jdd.assistant.api.exception;

import com.springai.jdd.assistant.api.QueryResponseFactory;
import com.springai.jdd.assistant.api.dto.QueryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Slf4j
@RestControllerAdvice
class QueryExceptionHandler {

    private static final String MALFORMED_REQUEST_MESSAGE = "The request body could not be read. Send a JSON object "
                                                            + "with a prompt field.";
    private static final String FAILURE_MESSAGE = "The service could not complete your query. Please try again.";

    @ExceptionHandler(InvalidQueryException.class)
    @ResponseStatus(BAD_REQUEST)
    QueryResponse handleInvalidQuery(InvalidQueryException exception) {
        return QueryResponseFactory.buildError(exception.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(BAD_REQUEST)
    QueryResponse handleUnreadableRequest() {
        return QueryResponseFactory.buildError(MALFORMED_REQUEST_MESSAGE);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    QueryResponse handleFailure(Exception exception) {
        log.error("Query failed", exception);
        return QueryResponseFactory.buildError(FAILURE_MESSAGE);
    }
}
