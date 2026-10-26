package vn.ueh.kltn.restaurant.realtime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class RealtimeEventListener {
    private final SseHub hub;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterCommit(RealtimeEvent event) {
        try {
            hub.broadcast(event);
        } catch (Exception exception) {
            // The transaction has committed. A notification failure must not become HTTP 500.
            log.warn("Could not dispatch SSE signal after commit: {}", exception.getMessage());
        }
    }
}
