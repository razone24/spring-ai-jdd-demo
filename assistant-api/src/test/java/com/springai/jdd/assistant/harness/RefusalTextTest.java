package com.springai.jdd.assistant.harness;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.springai.jdd.assistant.harness.RefusalReason.OUT_OF_SCOPE;
import static com.springai.jdd.assistant.harness.RefusalReason.UNINTELLIGIBLE;
import static org.assertj.core.api.Assertions.assertThat;

class RefusalTextTest {

    private static final String WRITTEN_CALL = "refuse(OUT_OF_SCOPE)";
    private static final String WRITTEN_CALL_WITH_QUOTES = "refuse(\"UNINTELLIGIBLE\")";
    private static final String WRITTEN_CALL_IN_A_SENTENCE = "I have to refuse( OUT_OF_SCOPE ) here.";
    private static final String UNKNOWN_REASON = "refuse(NO_SUCH_REASON)";
    private static final String ANSWER = "The after-party starts at 19:30 on the rooftop.";

    @ParameterizedTest
    @MethodSource("writtenCalls")
    void shouldDetectAToolCallTheModelWroteAsText(String content, RefusalReason expected) {
        assertThat(RefusalText.detect(content)).isEqualTo(expected);
    }

    private static Stream<Arguments> writtenCalls() {
        return Stream.of(Arguments.of(WRITTEN_CALL, OUT_OF_SCOPE),
                         Arguments.of(WRITTEN_CALL_WITH_QUOTES, UNINTELLIGIBLE),
                         Arguments.of(WRITTEN_CALL_IN_A_SENTENCE, OUT_OF_SCOPE));
    }

    @Test
    void shouldIgnoreAReasonThePlatformDoesNotHave() {
        assertThat(RefusalText.detect(UNKNOWN_REASON)).isNull();
    }

    @Test
    void shouldLeaveAnOrdinaryAnswerAlone() {
        assertThat(RefusalText.detect(ANSWER)).isNull();
        assertThat(RefusalText.detect(null)).isNull();
    }
}
