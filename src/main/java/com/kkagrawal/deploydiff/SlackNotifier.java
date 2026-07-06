package com.kkagrawal.deploydiff;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class SlackNotifier {

    private static final Logger log = LoggerFactory.getLogger(SlackNotifier.class);

    private final RestClient restClient;
    private final String webhookUrl;

    public SlackNotifier(RestClient restClient,
                         @Value("${slack.webhook-url:}") String webhookUrl) {
        this.restClient = restClient;
        this.webhookUrl = webhookUrl;
    }

    public void notifyDeployment(DeploymentDiff diff) {
        // If no webhook is configured, do nothing (safe for local dev / tests).
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }

        try {
            String text = buildMessage(diff);
            restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("text", text))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            // Notifications are best-effort: a failure here must NOT fail the deploy record.
            log.warn("Failed to send deploy notification: {}", e.getMessage());
        }
    }


    private String buildMessage(DeploymentDiff diff) {
        if (diff.firstDeploy()) {
            return String.format(
                    ":rocket: *First deploy* of `%s` to *%s*%n• version: `%s`%n• commit: `%s`",
                    diff.serviceName(), diff.environment(),
                    diff.currentVersion(), diff.currentCommit());
        }

        return String.format(
                ":rocket: *Deploy* of `%s` to *%s*%n"
                        + "• version: `%s` → `%s`%n"
                        + "• commit: `%s` → `%s`%n"
                        + "• %d min since last deploy",
                diff.serviceName(), diff.environment(),
                diff.previousVersion(), diff.currentVersion(),
                diff.previousCommit(), diff.currentCommit(),
                diff.minutesSinceLastDeploy());
    }
}
