package com.springai.jdd.conference.tools;

import org.springframework.ai.tool.execution.ToolCallResultConverter;

import java.lang.reflect.Type;

/**
 * Hands plain-text tool results to the model as they are. The default converter JSON-encodes every
 * result, which turns a readable agenda into one long escaped string ({@code "...\n..."}) that costs
 * more tokens and reads worse.
 */
public class PlainTextResultConverter implements ToolCallResultConverter {

    @Override
    public String convert(Object result, Type returnType) {
        return result == null ? "" : result.toString();
    }
}
