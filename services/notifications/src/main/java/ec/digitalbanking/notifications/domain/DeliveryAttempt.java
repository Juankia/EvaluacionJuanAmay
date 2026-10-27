package ec.digitalbanking.notifications.domain;

public record DeliveryAttempt(Channel channel, boolean sent, int attempts) {}
