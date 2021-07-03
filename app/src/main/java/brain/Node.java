package brain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.eventbus.MessageProducer;
import io.vertx.core.json.JsonObject;

public class Node implements Handler<Message<JsonObject>> {
    private final EventBus eventBus;
    private final MessageProducer<JsonObject> messageProducer;
    private final MessageConsumer<JsonObject> controlConsumer;
    private final Map<String,MessageConsumer<JsonObject>> messageConsumers;
    private Supplier<String> inputAddressSupplier;
    private Function<JsonObject, Optional<JsonObject>> reactor;

    // A Node has:
    // - input topics, that it listens to
    // - output topic, that it sends to
    // - internal logic determining what happens when a message is read on an input
    // topic
    public Node(EventBus eventBus, String outputAddress, Supplier<String> inputAddressSupplier, Function<JsonObject, Optional<JsonObject>> reactor) {
        this.eventBus = eventBus;
        this.messageProducer = eventBus.<JsonObject>publisher(outputAddress);
        this.inputAddressSupplier = inputAddressSupplier;
        this.reactor = reactor;
        this.messageConsumers = new HashMap<>();
        this.controlConsumer = eventBus.<JsonObject>consumer("control").handler(this);
        
    }

    private void handleControlTick() {
        String newInputAddress = inputAddressSupplier.get();
        if (!messageConsumers.containsKey(newInputAddress)) {
            MessageConsumer<JsonObject> newConsumer = eventBus.<JsonObject>consumer(newInputAddress).handler(this);
            messageConsumers.put(newInputAddress, newConsumer);
        }
    }

    private void handleControl(JsonObject jsonObject) {
        String command = jsonObject.getString("command");
        switch (command) {
            case "tick" -> handleControlTick();
            default -> throw new IllegalStateException("No such command: "+command);
        }
    }

    @Override
    public void handle(Message<JsonObject> event) {
        if (event.address().equals("control")) {
            handleControl(event.body());
        } else {
            Optional<JsonObject> reaction = reactor.apply(event.body());
            reaction.ifPresent(json -> {
                messageProducer.write(json);
            });
        }
    }

    public Future<Void> close() {
        List<Future> closing = new ArrayList<>();
        closing.add(messageProducer.close());
        closing.add(controlConsumer.unregister());
        for (var messageConsumer : messageConsumers.values()) {
            closing.add(messageConsumer.unregister());
        }
       return CompositeFuture.all(closing).compose(v -> Future.<Void>succeededFuture());
    }

}
