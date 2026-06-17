package br.com.artheus.kairos.shared.contract.notification;

import br.com.artheus.kairos.shared.enums.RiskLevel;

public interface NotificationSender {

    void sendNotification(String cityName, RiskLevel riskLevel);
}