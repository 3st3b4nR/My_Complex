package com.mycomplex.tasks.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TaskStatusTest {

    @Test
    void permitsOnlyForwardAdministrativeTransitions() {
        assertThat(TaskStatus.PENDING.canTransitionTo(TaskStatus.IN_PROGRESS))
                .isTrue();
        assertThat(TaskStatus.IN_PROGRESS.canTransitionTo(TaskStatus.COMPLETED))
                .isTrue();
        assertThat(TaskStatus.PENDING.canTransitionTo(TaskStatus.COMPLETED))
                .isFalse();
        assertThat(TaskStatus.COMPLETED.canTransitionTo(TaskStatus.CANCELLED))
                .isFalse();
        assertThat(TaskStatus.CANCELLED.isTerminal()).isTrue();
    }
}
