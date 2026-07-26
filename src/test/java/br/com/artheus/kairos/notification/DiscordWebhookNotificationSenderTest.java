package br.com.artheus.kairos.notification;

import br.com.artheus.kairos.shared.enums.RiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiscordWebhookNotificationSender Unit Tests")
class DiscordWebhookNotificationSenderTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private WebClient webClient;

    @InjectMocks
    private DiscordWebhookNotificationSender notificationSender;

    private final String mockWebhookUrl = "https://discord.com/api/webhooks/mock-id/mock-token";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notificationSender, "webhookUrl", mockWebhookUrl);
    }

    @Nested
    @DisplayName("Tests for sendNotification")
    class SendNotificationTests {

        private final String message = "Alert: City Joinville reached a risk level of HIGH!";

        @Test
        @DisplayName("Should send Discord notification successfully when WebClient call completes")
        void shouldSendNotificationSuccessfully() {
            when(webClient.post()
                    .uri(mockWebhookUrl)
                    .bodyValue(Map.of("content", message))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block())
                    .thenReturn(null);

            assertThatCode(() -> notificationSender.sendNotification(message))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should capture exception gracefully and prevent process failure when WebClient fails")
        void shouldHandleExceptionGracefullyWhenWebClientFails() {
            WebClientResponseException discordError = new WebClientResponseException(
                    500, "Internal Server Error", null, null, null
            );

            when(webClient.post()
                    .uri(any(String.class))
                    .bodyValue(any())
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block())
                    .thenThrow(discordError);

            assertThatCode(() -> notificationSender.sendNotification(message))
                    .doesNotThrowAnyException();
        }
    }
}