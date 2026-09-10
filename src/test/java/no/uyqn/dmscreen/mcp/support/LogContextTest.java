package no.uyqn.dmscreen.mcp.support;

import java.util.UUID;
import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

class LogContextTest {
    private static final Logger log = LoggerFactory.getLogger(LogContextTest.class);

    private final UUID campaignId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();

    private LogContext getLogContext() {
        return LogContext.of(campaignId, sessionId);
    }

    @Test
    void setsAndClearsMdc() {
        try (var ignored = getLogContext()) {
            AssertThat.campaignId().isEqualTo(String.valueOf(campaignId));
            AssertThat.sessionId().isEqualTo(String.valueOf(sessionId));
        }
        AssertThat.campaignId().isNull();
        AssertThat.sessionId().isNull();
    }

    @Test
    void clearsOnException() {
        Assertions.assertThatThrownBy(() -> {
                    try (var ignored = getLogContext()) {
                        throw new IllegalStateException("Tool failed");
                    }
                })
                .isInstanceOf(IllegalStateException.class);
        AssertThat.campaignId().isNull();
        AssertThat.sessionId().isNull();
    }

    @Test
    void leavesOtherMdcEntriesAlone() {
        MDC.put("requestId", "r-1");
        try (var ignored = getLogContext()) {
            log.debug("Empty try block");
        }
        Assertions.assertThat(MDC.get("requestId")).isEqualTo("r-1");
        MDC.remove("requestId");
    }

    @Test
    void restoresOuterScopeWhenNested() {
        var innerCampaignId = UUID.randomUUID();
        var innerSessionId = UUID.randomUUID();

        try (var outer = getLogContext()) {
            try (var inner = LogContext.of(innerCampaignId, innerSessionId)) {
                AssertThat.campaignId().isEqualTo(String.valueOf(innerCampaignId));
                AssertThat.sessionId().isEqualTo(String.valueOf(innerSessionId));
            }
            AssertThat.campaignId().isEqualTo(String.valueOf(campaignId));
            AssertThat.sessionId().isEqualTo(String.valueOf(sessionId));
        }
        AssertThat.campaignId().isNull();
        AssertThat.sessionId().isNull();
    }

    @Test
    void rejectsNullIds() {
        Assertions.assertThatNullPointerException()
                .isThrownBy(() -> LogContext.of(null, sessionId))
                .withMessage("campaignId");
        Assertions.assertThatNullPointerException()
                .isThrownBy(() -> LogContext.of(campaignId, null))
                .withMessage("sessionId");
        AssertThat.campaignId().isNull();
        AssertThat.sessionId().isNull();
    }

    private static final class AssertThat {
        static AbstractStringAssert<?> campaignId() {
            return Assertions.assertThat(MDC.get(LogContext.CAMPAIGN_ID));
        }

        static AbstractStringAssert<?> sessionId() {
            return Assertions.assertThat(MDC.get(LogContext.SESSION_ID));
        }
    }
}
