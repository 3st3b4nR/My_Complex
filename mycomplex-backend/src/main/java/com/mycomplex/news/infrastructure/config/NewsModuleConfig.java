package com.mycomplex.news.infrastructure.config;

import java.time.Clock;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.mycomplex.news.application.port.in.CreateNewsUseCase;
import com.mycomplex.news.application.port.in.DeleteNewsUseCase;
import com.mycomplex.news.application.port.in.ListNewsUseCase;
import com.mycomplex.news.application.port.in.LoadNewsImageUseCase;
import com.mycomplex.news.application.port.in.UpdateNewsUseCase;
import com.mycomplex.news.application.port.out.LoadAuthorizedNewsPublisherPort;
import com.mycomplex.news.application.port.out.LoadAuthorizedNewsViewerPort;
import com.mycomplex.news.application.port.out.LoadNewsPort;
import com.mycomplex.news.application.port.out.LoadNewsImageContentPort;
import com.mycomplex.news.application.port.out.NewsImageStoragePort;
import com.mycomplex.news.application.port.out.PublishNewsEventPort;
import com.mycomplex.news.application.port.out.SaveNewsPort;
import com.mycomplex.news.application.port.out.ScheduleNewsImageDeletionPort;
import com.mycomplex.news.application.port.out.UpdateNewsPort;
import com.mycomplex.news.application.service.CreateNewsService;
import com.mycomplex.news.application.service.DeleteNewsService;
import com.mycomplex.news.application.service.ListNewsService;
import com.mycomplex.news.application.service.LoadNewsImageService;
import com.mycomplex.news.application.service.UpdateNewsService;

@Configuration(proxyBeanMethods = false)
public class NewsModuleConfig {

    @Bean
    @Primary
    Clock utcClock() {
        return Clock.systemUTC();
    }

    @Bean
    CreateNewsUseCase createNewsUseCase(
            LoadAuthorizedNewsPublisherPort publisherPort,
            SaveNewsPort saveNewsPort,
            NewsImageStoragePort imageStoragePort,
            PublishNewsEventPort eventPort,
            @Qualifier("utcClock") Clock utcClock,
            PlatformTransactionManager transactionManager
    ) {
        CreateNewsService service = new CreateNewsService(
                publisherPort,
                saveNewsPort,
                imageStoragePort,
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
    DeleteNewsUseCase deleteNewsUseCase(
            LoadAuthorizedNewsPublisherPort publisherPort,
            LoadNewsPort loadNewsPort,
            UpdateNewsPort updateNewsPort,
            ScheduleNewsImageDeletionPort imageDeletionPort,
            @Qualifier("utcClock") Clock utcClock,
            PlatformTransactionManager transactionManager
    ) {
        DeleteNewsService service = new DeleteNewsService(
                publisherPort,
                loadNewsPort,
                updateNewsPort,
                imageDeletionPort,
                utcClock
        );

        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);

        return command -> transaction.executeWithoutResult(
                status -> service.delete(command)
        );
    }

    @Bean
    ListNewsUseCase listNewsUseCase(
            LoadAuthorizedNewsViewerPort viewerPort,
            LoadNewsPort loadNewsPort,
            PlatformTransactionManager transactionManager
    ) {
        ListNewsService service = new ListNewsService(
                viewerPort,
                loadNewsPort
        );

        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);

        transaction.setReadOnly(true);

        return query -> transaction.execute(
                status -> service.list(query)
        );
    }

    @Bean
    LoadNewsImageUseCase loadNewsImageUseCase(
            LoadAuthorizedNewsViewerPort viewerPort,
            LoadNewsPort loadNewsPort,
            LoadNewsImageContentPort imageContentPort
    ) {
        return new LoadNewsImageService(
                viewerPort,
                loadNewsPort,
                imageContentPort
        );
    }

    @Bean
    UpdateNewsUseCase updateNewsUseCase(
            LoadAuthorizedNewsPublisherPort publisherPort,
            LoadNewsPort loadNewsPort,
            UpdateNewsPort updateNewsPort,
            NewsImageStoragePort imageStoragePort,
            PublishNewsEventPort eventPort,
            @Qualifier("utcClock") Clock utcClock,
            PlatformTransactionManager transactionManager
    ) {
        UpdateNewsService service = new UpdateNewsService(
                publisherPort,
                loadNewsPort,
                updateNewsPort,
                imageStoragePort,
                eventPort,
                utcClock
        );

        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);

        return command -> transaction.execute(
                status -> service.update(command)
        );
    }
}
