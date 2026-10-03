package com.springai.jdd.assistant.agent.trail;

/**
 * Who decided to call a tool: the model, during its tool loop, or the harness around it
 * (the semantic cache look-up and write-back are deterministic steps, not model choices).
 */
public enum ToolOrigin {
    MODEL,
    HARNESS
}
