package br.com.artheus.kairos.notification;

import br.com.artheus.kairos.shared.contract.notification.NotificationSender;
import br.com.artheus.kairos.shared.enums.RiskLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DiscordWebhookNotificationSender implements NotificationSender {

    private final WebClient webClient;

    @Value("${discord.webhook.url}")
    private String webhookUrl;

    @Override
    public void sendNotification(String cityName, RiskLevel riskLevel) {
        try {
            String message = String.format("Alert: City %s reached a risk level of %s!", cityName, riskLevel.toString());
            Map<String, String> payload = Map.of("content", message);

            log.info("Sending Discord notification for {}...", cityName);

            webClient.post()
                    .uri(webhookUrl)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();

            log.info("Notification sent successfully to {}", cityName);

        } catch (Exception e) {
            log.error("Failed to send Discord notification for {}. Continuing process. Error: {}", cityName, e.getMessage());
        }
    }
}