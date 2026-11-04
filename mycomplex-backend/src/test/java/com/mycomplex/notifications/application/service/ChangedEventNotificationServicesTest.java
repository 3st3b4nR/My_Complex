package com.mycomplex.notifications.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.mycomplex.meetings.domain.event.MeetingChangedEvent;
import com.mycomplex.news.domain.event.NewsChangedEvent;
import com.mycomplex.notifications.application.exception.NotificationDeliveryException;
import com.mycomplex.notifications.application.model.EmailNotification;
import com.mycomplex.notifications.application.model.NotificationRecipient;
import com.mycomplex.notifications.application.model.PushNotification;
import com.mycomplex.notifications.application.port.out.LoadMeetingNotificationRecipientsPort;
import com.mycomplex.notifications.application.port.out.LoadNewsNotificationRecipientsPort;
import com.mycomplex.notifications.application.port.out.SendEmailNotificationPort;
import com.mycomplex.notifications.application.port.out.SendPushNotificationPort;

class ChangedEventNotificationServicesTest {
    private static final UUID PROPERTY_ID = UUID.randomUUID();
    private static final UUID ITEM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-11-03T15:00:00Z");
    private static final List<NotificationRecipient> RECIPIENTS = List.of(
            new NotificationRecipient(USER_ID, "owner@example.com"),
            new NotificationRecipient(USER_ID, "owner@example.com")
    );

    @Test
    void sendsDeduplicatedEmailAndPushForCreatedNews() {
        LoadNewsNotificationRecipientsPort recipients =
                mock(LoadNewsNotificationRecipientsPort.class);
        SendEmailNotificationPort email = mock(SendEmailNotificationPort.class);
        SendPushNotificationPort push = mock(SendPushNotificationPort.class);
        when(recipients.loadActiveByPropertyId(PROPERTY_ID))
                .thenReturn(RECIPIENTS);
        NotifyNewsChangedService service = new NotifyNewsChangedService(
                recipients, List.of(email), List.of(push));

        service.notify(newsEvent(NewsChangedEvent.ChangeType.CREATED));

        ArgumentCaptor<EmailNotification> emailCaptor =
                ArgumentCaptor.forClass(EmailNotification.class);
        verify(email).send(emailCaptor.capture());
        assertThat(emailCaptor.getValue().recipients())
                .containsExactly("owner@example.com");
        assertThat(emailCaptor.getValue().subject()).contains("Nueva noticia");
        assertThat(emailCaptor.getValue().deliveryKey())
                .contains("news-", "-created-");
        ArgumentCaptor<PushNotification> pushCaptor =
                ArgumentCaptor.forClass(PushNotification.class);
        verify(push).send(pushCaptor.capture());
        assertThat(pushCaptor.getValue().recipientIds())
                .containsExactly(USER_ID);
        assertThat(pushCaptor.getValue().data())
                .containsEntry("type", "NEWS_CREATED")
                .containsEntry("propertyId", PROPERTY_ID.toString());
    }

    @Test
    void supportsUpdatedNewsAndReturnsWhenThereAreNoRecipients() {
        LoadNewsNotificationRecipientsPort recipients =
                mock(LoadNewsNotificationRecipientsPort.class);
        SendEmailNotificationPort email = mock(SendEmailNotificationPort.class);
        when(recipients.loadActiveByPropertyId(PROPERTY_ID))
                .thenReturn(RECIPIENTS, List.of());
        NotifyNewsChangedService service = new NotifyNewsChangedService(
                recipients, List.of(email), List.of());

        service.notify(newsEvent(NewsChangedEvent.ChangeType.UPDATED));
        ArgumentCaptor<EmailNotification> captor =
                ArgumentCaptor.forClass(EmailNotification.class);
        verify(email).send(captor.capture());
        assertThat(captor.getValue().subject()).contains("actualizada");

        service.notify(newsEvent(NewsChangedEvent.ChangeType.CREATED));
    }

    @Test
    void combinesNewsDeliveryFailuresAfterTryingEveryChannel() {
        LoadNewsNotificationRecipientsPort recipients =
                mock(LoadNewsNotificationRecipientsPort.class);
        SendEmailNotificationPort firstEmail = mock(SendEmailNotificationPort.class);
        SendEmailNotificationPort secondEmail = mock(SendEmailNotificationPort.class);
        SendPushNotificationPort push = mock(SendPushNotificationPort.class);
        when(recipients.loadActiveByPropertyId(PROPERTY_ID)).thenReturn(RECIPIENTS);
        RuntimeException first = new IllegalStateException("email");
        RuntimeException second = new IllegalArgumentException("email-2");
        org.mockito.Mockito.doThrow(first).when(firstEmail).send(any());
        org.mockito.Mockito.doThrow(second).when(secondEmail).send(any());
        org.mockito.Mockito.doThrow(new IllegalStateException("push"))
                .when(push).send(any());
        NotifyNewsChangedService service = new NotifyNewsChangedService(
                recipients, List.of(firstEmail, secondEmail), List.of(push));

        assertThatThrownBy(() -> service.notify(
                newsEvent(NewsChangedEvent.ChangeType.CREATED)))
                .isInstanceOf(NotificationDeliveryException.class)
                .hasCause(first);
        assertThat(first.getSuppressed()).hasSize(2);
        verify(push).send(any());
    }

    @Test
    void sendsCreatedUpdatedAndReminderMeetingMessages() {
        LoadMeetingNotificationRecipientsPort recipients =
                mock(LoadMeetingNotificationRecipientsPort.class);
        SendEmailNotificationPort email = mock(SendEmailNotificationPort.class);
        SendPushNotificationPort push = mock(SendPushNotificationPort.class);
        when(recipients.loadActiveByPropertyId(PROPERTY_ID)).thenReturn(RECIPIENTS);
        NotifyMeetingChangedService service = new NotifyMeetingChangedService(
                recipients, List.of(email), List.of(push));

        for (MeetingChangedEvent.ChangeType type
                : MeetingChangedEvent.ChangeType.values()) {
            service.notify(meetingEvent(type));
        }

        ArgumentCaptor<EmailNotification> emails =
                ArgumentCaptor.forClass(EmailNotification.class);
        verify(email, org.mockito.Mockito.times(3)).send(emails.capture());
        assertThat(emails.getAllValues()).extracting(EmailNotification::subject)
                .anyMatch(value -> value.contains("Nueva reunión"))
                .anyMatch(value -> value.contains("actualizada"))
                .anyMatch(value -> value.contains("Recordatorio"));
        assertThat(emails.getAllValues()).allSatisfy(notification ->
                assertThat(notification.body()).contains("https://meet.example/1"));
        ArgumentCaptor<PushNotification> pushes =
                ArgumentCaptor.forClass(PushNotification.class);
        verify(push, org.mockito.Mockito.times(3)).send(pushes.capture());
        assertThat(pushes.getAllValues().get(2).data())
                .containsEntry("type", "MEETING_REMINDER")
                .containsEntry("meetingUrl", "https://meet.example/1");
    }

    @Test
    void skipsEmptyMeetingRecipientsAndReportsCombinedFailures() {
        LoadMeetingNotificationRecipientsPort recipients =
                mock(LoadMeetingNotificationRecipientsPort.class);
        SendEmailNotificationPort email = mock(SendEmailNotificationPort.class);
        SendPushNotificationPort push = mock(SendPushNotificationPort.class);
        when(recipients.loadActiveByPropertyId(PROPERTY_ID))
                .thenReturn(List.of(), RECIPIENTS);
        NotifyMeetingChangedService service = new NotifyMeetingChangedService(
                recipients, List.of(email), List.of(push));
        service.notify(meetingEvent(MeetingChangedEvent.ChangeType.CREATED));
        verify(email, never()).send(any());

        RuntimeException emailFailure = new IllegalStateException("email");
        org.mockito.Mockito.doThrow(emailFailure).when(email).send(any());
        org.mockito.Mockito.doThrow(new IllegalStateException("push"))
                .when(push).send(any());
        assertThatThrownBy(() -> service.notify(
                meetingEvent(MeetingChangedEvent.ChangeType.UPDATED)))
                .isInstanceOf(NotificationDeliveryException.class)
                .hasCause(emailFailure);
        assertThat(emailFailure.getSuppressed()).hasSize(1);
    }

    private NewsChangedEvent newsEvent(NewsChangedEvent.ChangeType type) {
        return new NewsChangedEvent(
                ITEM_ID, PROPERTY_ID, UUID.randomUUID(), "Aviso", type, NOW);
    }

    private MeetingChangedEvent meetingEvent(MeetingChangedEvent.ChangeType type) {
        return new MeetingChangedEvent(
                ITEM_ID, PROPERTY_ID, "Asamblea", "Presupuesto",
                URI.create("https://meet.example/1"), NOW.plusSeconds(86400),
                type, NOW);
    }
}
