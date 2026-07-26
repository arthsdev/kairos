package br.com.artheus.kairos.notification;

import br.com.artheus.kairos.shared.contract.notification.NotificationSender;
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
    public void sendNotification(String message) {
        try {
            Map<String, String> payload = Map.of("content", message);

            webClient.post()
                    .uri(webhookUrl)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();

            log.info("Notification sent successfully. Message: {}.", message);

        } catch (Exception e) {
            log.error("Failed to send Discord notification. Message: {}. Error: {}", message, e.getMessage(), e);
        }
    }
}