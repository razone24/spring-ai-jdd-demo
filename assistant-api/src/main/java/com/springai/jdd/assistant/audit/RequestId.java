package com.springai.jdd.assistant.audit;

import lombok.NoArgsConstructor;
import org.slf4j.MDC;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public final class RequestId {

    public static final String KEY = "requestId";

    public static String current() {
        return MDC.get(KEY);
    }
}
