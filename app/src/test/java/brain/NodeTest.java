package brain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageProducer;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class NodeTest {
    private static EventBus eventBus;
    private static MessageProducer<JsonObject> messageProducer;

    @BeforeAll
    public static void setupAll(Vertx vertx, VertxTestContext testContext) {
        eventBus = vertx.eventBus();
        messageProducer = eventBus.publisher(Channels.CONTROL);
        testContext.completeNow();
    }

    @Test
    public void testCreateOpen(Vertx vertx, VertxTestContext testContext) {
        Node node = new Node(eventBus, json -> Optional.empty());
        testContext.verify(() -> {
            assertFalse(node.isClosed());
            testContext.completeNow();
        });
    }
    @Test
    public void testClose(Vertx vertx, VertxTestContext testContext) {
        Node node = new Node(eventBus, json -> Optional.empty());
        JsonObject message = new JsonObject();
        message.put(Constants.COMMAND, Commands.CLOSE);
        message.put(Constants.ADDRESS, node.getAddress());
        messageProducer.write(message).map(v -> testContext.verify(() -> {
            assertTrue(node.isClosed());
            testContext.completeNow();
        })).onFailure(ex -> testContext.failNow(ex));
    }

}
