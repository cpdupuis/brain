package brain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

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
    private final String outputAddress;
    private final MessageProducer<JsonObject> messageProducer;
    private final MessageConsumer<JsonObject> controlConsumer;
    private final Map<String,MessageConsumer<JsonObject>> messageConsumers;
    private Function<JsonObject, Optional<JsonObject>> reactor;
    private boolean wantAddTopic;

    // A Node has:
    // - input topics, that it listens to
    // - output topic, that it sends to
    // - internal logic determining what happens when a message is read on an input
    // topic
    public Node(EventBus eventBus, Function<JsonObject, Optional<JsonObject>> reactor) {
        this.eventBus = eventBus;
        this.outputAddress = UUID.randomUUID().toString();
        this.messageProducer = eventBus.<JsonObject>publisher(outputAddress);
        this.reactor = reactor;
        this.messageConsumers = new HashMap<>();
        this.controlConsumer = eventBus.<JsonObject>consumer(Channels.CONTROL).handler(this);
        this.wantAddTopic = true;
        
    }


    private void addInputAddress(String newInputAddress) {
        if (!messageConsumers.containsKey(newInputAddress)) {
            MessageConsumer<JsonObject> newConsumer = eventBus.<JsonObject>consumer(newInputAddress).handler(this);
            messageConsumers.put(newInputAddress, newConsumer);
        }
    }

    private void handleControlTick() {
        // Maybe we should advertise
        double d = ThreadLocalRandom.current().nextDouble();
        if (d < Constants.ADVERTISING_CHANCE) {
            JsonObject advertisement = new JsonObject();
            advertisement.put(Constants.ADDRESS, outputAddress);
            eventBus.publish(Channels.ADVERTISEMENT, advertisement);
        }
    }

    private void handleControl(JsonObject jsonObject) {
        String command = jsonObject.getString(Constants.COMMAND);
        switch (command) {
            case Commands.TICK -> handleControlTick();
            case Commands.CLOSE -> close();
            default -> throw new IllegalStateException("No such command: "+command);
        }
    }

    @Override
    public void handle(Message<JsonObject> event) {
        switch (event.address()) {
            case Channels.CONTROL -> handleControl(event.body());
            case Channels.ADVERTISEMENT -> handleAdvertisement(event.body());
            default -> {
                Optional<JsonObject> reaction = reactor.apply(event.body());
                reaction.ifPresent(json -> {
                    messageProducer.write(json);
                });
            }
        }
    }

    private void handleAdvertisement(JsonObject body) {
        if (wantAddTopic) {
            wantAddTopic = false;
            String newInputAddress = body.getString(Constants.ADDRESS);
            addInputAddress(newInputAddress);
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
