package no.uyqn.dmscreen.mcp.support;

import java.util.Objects;
import java.util.UUID;
import org.slf4j.MDC;

public final class LogContext implements AutoCloseable {
    public static final String CAMPAIGN_ID = "campaignId";
    public static final String SESSION_ID = "sessionId";

    private final String previousCampaignId;
    private final String previousSessionId;

    private LogContext(String previousCampaignId, String previousSessionId) {
        this.previousCampaignId = previousCampaignId;
        this.previousSessionId = previousSessionId;
    }

    public static LogContext of(UUID campaignId, UUID sessionId) {
        Objects.requireNonNull(campaignId, "campaignId");
        Objects.requireNonNull(sessionId, "sessionId");
        var scope = new LogContext(MDC.get(CAMPAIGN_ID), MDC.get(SESSION_ID));
        MDC.put(CAMPAIGN_ID, campaignId.toString());
        MDC.put(SESSION_ID, sessionId.toString());
        return scope;
    }

    private static void restore(String key, String previous) {
        if (previous == null) MDC.remove(key);
        else MDC.put(key, previous);
    }

    @Override
    public void close() {
        restore(CAMPAIGN_ID, previousCampaignId);
        restore(SESSION_ID, previousSessionId);
    }
}
