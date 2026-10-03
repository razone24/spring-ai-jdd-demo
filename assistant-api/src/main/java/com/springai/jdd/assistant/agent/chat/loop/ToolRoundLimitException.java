package com.springai.jdd.assistant.agent.chat.loop;

public class ToolRoundLimitException extends RuntimeException {

    ToolRoundLimitException(String message) {
        super(message);
    }
}
