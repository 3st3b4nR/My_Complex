package com.mycomplex.news.infrastructure.adapter.out.blob;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.mycomplex.news.application.port.out.NewsImageStoragePort;
import com.mycomplex.news.application.port.out.ScheduleNewsImageDeletionPort;
import com.mycomplex.news.domain.model.NewsImage;

@Component
public class AfterCommitNewsImageDeletionAdapter
        implements ScheduleNewsImageDeletionPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            AfterCommitNewsImageDeletionAdapter.class
    );

    private final NewsImageStoragePort imageStoragePort;

    public AfterCommitNewsImageDeletionAdapter(
            NewsImageStoragePort imageStoragePort
    ) {
        this.imageStoragePort = imageStoragePort;
    }

    @Override
    public void schedule(NewsImage image) {
        if (image == null) {
            return;
        }

        if (!TransactionSynchronizationManager
                .isSynchronizationActive()) {
            deleteSafely(image);
            return;
        }

        TransactionSynchronizationManager
                .registerSynchronization(
                        new TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                deleteSafely(image);
                            }
                        }
                );
    }

    private void deleteSafely(NewsImage image) {
        try {
            imageStoragePort.delete(image);
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "Could not delete news image {} after commit",
                    image.blobPath(),
                    exception
            );
        }
    }
}
