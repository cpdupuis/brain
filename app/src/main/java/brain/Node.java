package brain;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;

public class Node {

    // A Node has:
    // - input topics, that it listens to
    // - output topic, that it sends to
    // - internal logic determining what happens when a message is read on an input
    // topic
    public Node(EventBus eventBus, String outputAddress, Supplier<String> inputAddressSupplier, Function<JsonObject, Optional<JsonObject>> reactor) {
    }

}
