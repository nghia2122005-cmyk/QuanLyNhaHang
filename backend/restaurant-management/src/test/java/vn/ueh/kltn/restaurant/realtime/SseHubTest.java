package vn.ueh.kltn.restaurant.realtime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import vn.ueh.kltn.restaurant.config.RealtimeConfig;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

class SseHubTest {
    static Stream<Exception> disconnectedErrors() {
        return Stream.of(
                new IOException("disconnected"),
                new IllegalStateException("already complete"),
                new RuntimeException("unexpected converter failure"));
    }

    @ParameterizedTest
    @MethodSource("disconnectedErrors")
    @SuppressWarnings("unchecked")
    void brokenClientIsRemovedWithoutInterruptingHealthyClientsOrCompletingAgain(Exception failure)
            throws IOException {
        var hub = new SseHub(new SyncTaskExecutor());
        SseEmitter disconnected = mock(SseEmitter.class);
        SseEmitter healthy = mock(SseEmitter.class);
        List<SseEmitter> emitters =
                (List<SseEmitter>) ReflectionTestUtils.getField(hub, "emitters");
        emitters.add(disconnected);
        emitters.add(healthy);
        doThrow(failure).when(disconnected).send(any(SseEmitter.SseEventBuilder.class));
        // A second completion on an errored AsyncContext is illegal on Tomcat.
        doThrow(new IllegalStateException("container already completed"))
                .when(disconnected)
                .complete();
        hub.broadcast(new RealtimeEvent(RealtimeEvent.Type.ORDER, 7L));
        hub.heartbeat();
        verify(disconnected, times(1)).send(any(SseEmitter.SseEventBuilder.class));
        verify(disconnected, never()).complete();
        verify(healthy, times(2)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void slowClientDoesNotBlockCallerOrHealthyClientUsingConfiguredExecutor() throws Exception {
        var release = new CountDownLatch(1);
        try (var context =
                new AnnotationConfigApplicationContext(RealtimeConfig.class, SseHub.class)) {
            SseHub hub = context.getBean(SseHub.class);
            SseEmitter slow = mock(SseEmitter.class);
            SseEmitter healthy = mock(SseEmitter.class);
            var entered = new CountDownLatch(1);
            var delivered = new CountDownLatch(1);
            List<SseEmitter> emitters =
                    (List<SseEmitter>) ReflectionTestUtils.getField(hub, "emitters");
            emitters.add(slow);
            emitters.add(healthy);
            doAnswer(
                            invocation -> {
                                entered.countDown();
                                release.await(5, TimeUnit.SECONDS);
                                return null;
                            })
                    .when(slow)
                    .send(any(SseEmitter.SseEventBuilder.class));
            doAnswer(
                            invocation -> {
                                delivered.countDown();
                                return null;
                            })
                    .when(healthy)
                    .send(any(SseEmitter.SseEventBuilder.class));
            assertTimeoutPreemptively(
                    Duration.ofSeconds(1),
                    () -> hub.broadcast(new RealtimeEvent(RealtimeEvent.Type.ORDER, 7L)));
            assertTrue(entered.await(1, TimeUnit.SECONDS));
            assertTrue(
                    delivered.await(1, TimeUnit.SECONDS),
                    "Healthy client must receive while slow client is still blocked");
            release.countDown();
        } finally {
            release.countDown();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void rejectedExecutorNeverRunsSendsOnCallerOrPropagatesToCommittedRequest() {
        TaskExecutor executor = mock(TaskExecutor.class);
        doThrow(new RejectedExecutionException("queue full"))
                .when(executor)
                .execute(any(Runnable.class));
        var hub = new SseHub(executor);
        SseEmitter emitter = mock(SseEmitter.class);
        List<SseEmitter> emitters =
                (List<SseEmitter>) ReflectionTestUtils.getField(hub, "emitters");
        emitters.add(emitter);
        assertDoesNotThrow(() -> hub.broadcast(new RealtimeEvent(RealtimeEvent.Type.ORDER, 7L)));
        assertDoesNotThrow(hub::heartbeat);
        verifyNoInteractions(emitter);
    }
}
