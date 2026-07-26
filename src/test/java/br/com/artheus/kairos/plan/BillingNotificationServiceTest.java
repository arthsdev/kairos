package br.com.artheus.kairos.plan;

import br.com.artheus.kairos.shared.contract.notification.NotificationSender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("BillingNotificationService Unit Tests")
class BillingNotificationServiceTest {

    @Mock
    private NotificationSender notificationSender;

    @InjectMocks
    private BillingNotificationService billingNotificationService;

    @Test
    @DisplayName("Should format and send payment failed notification")
    void shouldNotifyPaymentFailed() {
        String customerId = "cus_123";

        billingNotificationService.notifyPaymentFailed(customerId);

        verify(notificationSender, times(1))
                .sendNotification(contains(customerId));
    }

    @Test
    @DisplayName("Should format and send cancellation notification")
    void shouldNotifyCancellation() {
        String customerId = "cus_123";

        billingNotificationService.notifyCancellation(customerId);

        verify(notificationSender, times(1))
                .sendNotification(contains(customerId));
    }
}