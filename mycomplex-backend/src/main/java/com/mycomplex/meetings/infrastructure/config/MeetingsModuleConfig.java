package com.mycomplex.meetings.infrastructure.config;

import java.time.Clock;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.mycomplex.meetings.application.port.in.CreateMeetingUseCase;
import com.mycomplex.meetings.application.port.in.DeleteMeetingUseCase;
import com.mycomplex.meetings.application.port.in.ListMeetingsUseCase;
import com.mycomplex.meetings.application.port.in.SendMeetingRemindersUseCase;
import com.mycomplex.meetings.application.port.in.UpdateMeetingUseCase;
import com.mycomplex.meetings.application.port.out.LoadAuthorizedMeetingOrganizerPort;
import com.mycomplex.meetings.application.port.out.LoadAuthorizedMeetingViewerPort;
import com.mycomplex.meetings.application.port.out.MeetingPersistencePort;
import com.mycomplex.meetings.application.port.out.PublishMeetingEventPort;
import com.mycomplex.meetings.application.service.CreateMeetingService;
import com.mycomplex.meetings.application.service.DeleteMeetingService;
import com.mycomplex.meetings.application.service.ListMeetingsService;
import com.mycomplex.meetings.application.service.SendMeetingRemindersService;
import com.mycomplex.meetings.application.service.UpdateMeetingService;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class MeetingsModuleConfig {

    @Bean
    CreateMeetingUseCase createMeetingUseCase(
            LoadAuthorizedMeetingOrganizerPort organizerPort,
            MeetingPersistencePort persistencePort,
            PublishMeetingEventPort eventPort,
            @Qualifier("utcClock") Clock utcClock,
            PlatformTransactionManager transactionManager
    ) {
        CreateMeetingService service = new CreateMeetingService(
                organizerPort,
                persistencePort,
                eventPort,
                utcClock
        );
        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);
        return command -> transaction.execute(
                status -> service.create(command)
        );
    }

    @Bean
    UpdateMeetingUseCase updateMeetingUseCase(
            LoadAuthorizedMeetingOrganizerPort organizerPort,
            MeetingPersistencePort persistencePort,
            PublishMeetingEventPort eventPort,
            @Qualifier("utcClock") Clock utcClock,
            PlatformTransactionManager transactionManager
    ) {
        UpdateMeetingService service = new UpdateMeetingService(
                organizerPort,
                persistencePort,
                eventPort,
                utcClock
        );
        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);
        return command -> transaction.execute(
                status -> service.update(command)
        );
    }

    @Bean
    DeleteMeetingUseCase deleteMeetingUseCase(
            LoadAuthorizedMeetingOrganizerPort organizerPort,
            MeetingPersistencePort persistencePort,
            @Qualifier("utcClock") Clock utcClock,
            PlatformTransactionManager transactionManager
    ) {
        DeleteMeetingService service = new DeleteMeetingService(
                organizerPort,
                persistencePort,
                utcClock
        );
        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);
        return command -> transaction.executeWithoutResult(
                status -> service.delete(command)
        );
    }

    @Bean
    ListMeetingsUseCase listMeetingsUseCase(
            LoadAuthorizedMeetingViewerPort viewerPort,
            MeetingPersistencePort persistencePort,
            PlatformTransactionManager transactionManager
    ) {
        ListMeetingsService service = new ListMeetingsService(
                viewerPort,
                persistencePort
        );
        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);
        transaction.setReadOnly(true);
        return query -> transaction.execute(
                status -> service.list(query)
        );
    }

    @Bean
    SendMeetingRemindersUseCase sendMeetingRemindersUseCase(
            MeetingPersistencePort persistencePort,
            PublishMeetingEventPort eventPort,
            @Qualifier("utcClock") Clock utcClock,
            PlatformTransactionManager transactionManager
    ) {
        SendMeetingRemindersService service =
                new SendMeetingRemindersService(
                        persistencePort,
                        eventPort,
                        utcClock
                );
        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);
        return () -> transaction.executeWithoutResult(
                status -> service.sendDueReminders()
        );
    }
}
