package vn.ueh.kltn.restaurant.realtime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class RealtimeEventListenerTest {
    @Test
    void unexpectedDispatchFailureCannotTurnCommittedTransactionIntoFailure() {
        try (var context = new AnnotationConfigApplicationContext(TestConfig.class)) {
            SseHub hub = context.getBean(SseHub.class);
            var event = new RealtimeEvent(RealtimeEvent.Type.ORDER, 7L);
            doThrow(new RuntimeException("dispatch failure")).when(hub).broadcast(event);
            var template = new TransactionTemplate(new TestTransactionManager());
            assertDoesNotThrow(
                    () -> template.executeWithoutResult(status -> context.publishEvent(event)));
            verify(hub).broadcast(event);
        }
    }

    @Test
    void
            realSpringTransactionSynchronizationsDeliverOnlyAfterCommitNeverRollbackOrOutsideTransaction() {
        try (var context = new AnnotationConfigApplicationContext(TestConfig.class)) {
            SseHub hub = context.getBean(SseHub.class);
            var template = new TransactionTemplate(new TestTransactionManager());
            var committed = new RealtimeEvent(RealtimeEvent.Type.ORDER, 7L);
            template.executeWithoutResult(
                    status -> {
                        context.publishEvent(committed);
                        verifyNoInteractions(hub);
                    });
            verify(hub).broadcast(committed);
            assertThrows(
                    IllegalStateException.class,
                    () ->
                            template.executeWithoutResult(
                                    status -> {
                                        context.publishEvent(
                                                new RealtimeEvent(
                                                        RealtimeEvent.Type.FLOOR_PLAN, 99L));
                                        throw new IllegalStateException("rollback");
                                    }));
            template.executeWithoutResult(
                    status -> {
                        context.publishEvent(
                                new RealtimeEvent(RealtimeEvent.Type.RESERVATION, null));
                        status.setRollbackOnly();
                    });
            context.publishEvent(new RealtimeEvent(RealtimeEvent.Type.ORDER, 100L));
            verifyNoMoreInteractions(hub);
        }
    }

    @Configuration
    @EnableTransactionManagement
    @Import(RealtimeEventListener.class)
    static class TestConfig {
        @Bean
        SseHub hub() {
            return mock(SseHub.class);
        }
    }

    // Exercises Spring's commit/rollback event machinery without a database.
    static class TestTransactionManager extends AbstractPlatformTransactionManager {
        protected Object doGetTransaction() {
            return new Object();
        }

        protected void doBegin(Object transaction, TransactionDefinition definition) {}

        protected void doCommit(DefaultTransactionStatus status) {}

        protected void doRollback(DefaultTransactionStatus status) {}
    }
}
