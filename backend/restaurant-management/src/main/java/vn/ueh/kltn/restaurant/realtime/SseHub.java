package vn.ueh.kltn.restaurant.realtime;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
public class SseHub {
    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final TaskExecutor executor;

    public SseHub(@Qualifier("sseExecutor") TaskExecutor executor) {
        this.executor = executor;
    }

    public SseEmitter subscribe() {
        var emitter = new SseEmitter(30 * 60 * 1000L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(
                () -> {
                    emitters.remove(emitter);
                    emitter.complete();
                });
        emitter.onError(error -> emitters.remove(emitter));
        // Flush headers immediately so the client can detect opens/reconnects.
        send(emitter, SseEmitter.event().comment("connected"));
        return emitter;
    }

    public void broadcast(RealtimeEvent event) {
        for (SseEmitter emitter : emitters) {
            enqueue(emitter, SseEmitter.event().data(event));
        }
    }

    @Scheduled(fixedRate = 20_000)
    public void heartbeat() {
        for (SseEmitter emitter : emitters) {
            enqueue(emitter, SseEmitter.event().comment("ping"));
        }
    }

    private void enqueue(SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            executor.execute(() -> send(emitter, event));
        } catch (Exception exception) {
            // A saturated/shutting-down executor must never fail an already committed request.
            // Signals can be recovered by REST polling or reconnect invalidation.
            log.debug("SSE signal could not be queued: {}", exception.getMessage());
        }
    }

    private void send(SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (Exception exception) {
            emitters.remove(emitter);
            log.debug("SSE client disconnected: {}", exception.getMessage());
            // The container owns error completion after a failed send. Calling complete here
            // can throw again and interrupt delivery to the remaining healthy subscribers.
        }
    }
}
