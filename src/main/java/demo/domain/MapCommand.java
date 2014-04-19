package demo.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by sfrensley on 4/16/14.
 */
@Data
public class MapCommand {

    private String username;
    private Action action;
    private List<Double> coords;

    public MapCommand() {};

    public MapCommand(String username, List<Double> coords) {
        this.coords = coords;
        this.username = username;
    }

    public enum Action {

        MOVE("m"),
        DELETE("d"),
        CREATE("c");

        private final String value;

        private static final Map<String, Action> VALUE_MAP = new LinkedHashMap<>();
        static {
            for (Action action : Action.values()) {
                VALUE_MAP.put(action.value, action);
            }
        }

        //@JsonValue declares what method is used to serialize this enum
        @JsonValue
        public String toValue() {
            return this.value;
        }

        //@JsonCreator declares what method is used to deserialize this enum
        @JsonCreator
        public static Action fromValue(String value) {
            return VALUE_MAP.get(value);
        }


        private Action(final String value) {
            this.value = value;
        }
    }
}

