package brain;

import java.util.Optional;
import java.util.function.Function;

import io.vertx.core.json.JsonObject;

public class ReactorFunctions {
    public static final Function<JsonObject,Optional<JsonObject>> identity = json -> Optional.of(json);
    public static final Function<JsonObject,Optional<JsonObject>> delay = new Function<JsonObject,Optional<JsonObject>>(){
        JsonObject previous;
        @Override
        public Optional<JsonObject> apply(JsonObject t) {
           Optional<JsonObject> result = Optional.ofNullable(previous);
           previous = t;
           return result;
        }
    };
}
