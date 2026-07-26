package br.com.artheus.kairos.plan;

import br.com.artheus.kairos.shared.contract.notification.NotificationSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BillingNotificationService {

    private final NotificationSender notificationSender;

    public void notifyPaymentFailed(String customerId) {
        notificationSender.sendNotification("Payment failed for customer " + customerId);
    }

    public void notifyCancellation(String customerId) {
        notificationSender.sendNotification("Subscription canceled for customer " + customerId);
    }
}