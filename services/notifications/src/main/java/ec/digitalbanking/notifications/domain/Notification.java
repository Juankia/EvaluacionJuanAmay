package ec.digitalbanking.notifications.domain;

/**
 * A message to deliver. {@code templateKey} is resolved to localised text by the provider; the
 * notification never carries balances, amounts or contact data in clear (docs/25 §4.1).
 */
public record Notification(String eventId, String subjectId, String templateKey) {}
