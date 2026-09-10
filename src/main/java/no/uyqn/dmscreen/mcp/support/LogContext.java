package no.uyqn.dmscreen.mcp;

import org.slf4j.MDC;

public final class LogContext implements AutoCloseable {
    public static final String CAMPAIGN_ID = "campaignId";
    public static final String SESSION_ID = "sessionId";

    private LogContext() {}

    public static LogContext of(long campaignId, long sessionId) {
        MDC.put(CAMPAIGN_ID, String.valueOf(campaignId));
        MDC.put(SESSION_ID, String.valueOf(sessionId));
        return new LogContext();
    }

    @Override
    public void close() {
        MDC.remove(CAMPAIGN_ID);
        MDC.remove(SESSION_ID);
    }
}
