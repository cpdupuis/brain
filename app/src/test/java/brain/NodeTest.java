package brain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageProducer;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class NodeTest {
    private static EventBus eventBus;
    private static MessageProducer<JsonObject> controlMessageProducer;
    private static MessageProducer<JsonObject> advertisementMessageProducer;

    @BeforeAll
    public static void setupAll(Vertx vertx, VertxTestContext testContext) {
        eventBus = vertx.eventBus();
        controlMessageProducer = eventBus.publisher(Channels.CONTROL);
        advertisementMessageProducer = eventBus.publisher(Channels.ADVERTISEMENT);
        testContext.completeNow();
    }

    @Test
    public void testCreateOpen(Vertx vertx, VertxTestContext testContext) {
        Node node = new Node(eventBus, json -> Optional.empty());
        testContext.verify(() -> {
            assertFalse(node.isClosed());
            assertNotNull(node.getAddress());
            testContext.completeNow();
        });
    }

    @Test
    public void testClose(Vertx vertx, VertxTestContext testContext) {
        Node node = new Node(eventBus, json -> Optional.empty());
        JsonObject message = new JsonObject();
        message.put(Constants.COMMAND, Commands.CLOSE);
        message.put(Constants.ADDRESS, node.getAddress());
        controlMessageProducer.write(message).map(v -> testContext.verify(() -> {
            assertTrue(node.isClosed());
            testContext.completeNow();
        })).onFailure(ex -> testContext.failNow(ex));
    }

    @Test
    public void testAdvertise(Vertx vertx, VertxTestContext testContext) {
        Node node = new Node(eventBus, json -> Optional.empty());
        eventBus.<JsonObject>consumer(Channels.ADVERTISEMENT, msg -> {
            testContext.verify(() -> {
               assertEquals(node.getAddress(), msg.body().getString(Constants.ADDRESS));
               testContext.completeNow();
            });
        });
        node.setAdvertisingChance(1.0);
        JsonObject json = new JsonObject();
        json.put(Constants.COMMAND, Commands.TICK);
        controlMessageProducer.write(json);
    }

    @Test
    public void testRegister(Vertx vertx, VertxTestContext testContext) {
        Node node = new Node(eventBus, json -> Optional.empty());
        String address = "abc123";
        assertFalse(node.isListeningTopic(address));
        JsonObject json = new JsonObject();
        json.put(Constants.ADDRESS, address);
        advertisementMessageProducer.write(json).map(v -> testContext.verify(() -> {
            assertTrue(node.isListeningTopic(address));
            testContext.completeNow();
        })).onFailure(ex -> testContext.failNow(ex));

    }

    @Test
    public void testHandle(Vertx vertx, VertxTestContext vertxTestContext) {
        Node node = new Node(eventBus, json -> Optional.of(json));
        String nodeAddress = node.getAddress();
        JsonObject message = new JsonObject();
        message.put("hello", "world");
        eventBus.<JsonObject>consumer(nodeAddress).handler(msg -> vertxTestContext.verify(() -> {
            assertEquals("world", msg.body().getString("hello"));
            vertxTestContext.completeNow();
        }));
        String address = "abc123";
        JsonObject json = new JsonObject();
        json.put(Constants.ADDRESS, address);
        advertisementMessageProducer.write(json).flatMap(v -> {
            eventBus.publish(address, message);
            return Future.succeededFuture();
        });
    }

}
