package demo.service;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import demo.domain.MapCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import demo.domain.MapCommand.*;

/**
 * Created by sfrensley on 4/16/14.
 */
@Controller
public class MapService {
    private static final Logger logger = LoggerFactory.getLogger(MapService.class);

    private final Map<String, String> sessionCache = new ConcurrentHashMap<>();
    private final LoadingCache<String, UserMarker> markerCache = CacheBuilder
            .newBuilder().build(new CacheLoader<String, UserMarker>() {

                /**
                 * Cache miss results in creation of new marker for user.
                 * @param username
                 * @return
                 * @throws Exception
                 */
                @Override
                public UserMarker load(String username) throws Exception {
                    return new UserMarker(username);
                }

            });

    private SimpMessagingTemplate messagingTemplate;

    public static final String MAP_COMMAND_PATH = "/map/command";
    public static final String MAP_ITEM_PATH = "/map/items";
    public static final String MAP_UPDATE_PATH = "/topic/map/update";
    public static final String MAP_ERROR_PATH = "/queue/errors";

    @Autowired
    public void  setSimpMessaingTemplate(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Incoming request for all current markers
     * @param principal
     * @return
     * @throws Exception
     */
    @SubscribeMapping(MAP_ITEM_PATH)
    public Collection<MapService.UserMarker> getMapItems(Principal principal) throws Exception {

        return getUserMarkers();
    }

    /**
     * Incoming request to move or remove marker
     * @param command
     * @param principal
     */

    @MessageMapping(MAP_COMMAND_PATH)
    public void executeMapCommand(MapCommand command, Principal principal) {
        switch (command.getAction()) {
            case MOVE:
                updateMarkerPosition(command.getCoords(), principal.getName());
                break;
            case DELETE:
                removeMarker(principal.getName());
                break;
            default:
                break;
        }
    }

    /**
     * Error path for for logging error on the message bus
     * @param exception
     * @return
     */
    @MessageExceptionHandler
    @SendToUser(MAP_ERROR_PATH)
    public String handleException(Throwable exception) {
        return exception.getMessage();
    }

    /**
     * Update the marker cache and send new positions to all connected clients
     * @param coords
     * @param username
     */
    public void updateMarkerPosition(List<Double> coords, String username) {
        MapCommand command = new MapCommand(username, coords);
        //command m for move
        command.setAction(Action.MOVE);
        UserMarker marker = markerCache.getUnchecked(username);
        marker.setCoords(coords);

        sendMapCommand(command);
    }

    /**
     * Update the user in the marker cache and send new positions to all connected clients
     * @param coords
     * @param username
     */
    public void createMarker(List<Double> coords, String username) {
        MapCommand command = new MapCommand(username, coords);
        //command c for create
        command.setAction(Action.CREATE);

        markerCache.put(username, new UserMarker(username, coords));
        sendMapCommand(command);
    }

    /**
     * Remove the the user's marker from the marker cache
     * @param username
     */
    public void removeMarker(String username) {
        MapCommand command = new MapCommand(username, null);
        //command d for move
        command.setAction(Action.DELETE);
        markerCache.invalidate(username);
        sendMapCommand(command);
    }

    /**
     * Convert cache into Map
     * @return
     */
    public Collection<UserMarker> getUserMarkers() {
        return markerCache.asMap().values();
    }

    /**
     * Retreieve username for websocket session
     * @param id
     * @return
     */
    public String getUserForSession(String id) {
        return sessionCache.get(id);
    }

    /**
     * Add username to websocket session cache
     * @param id
     * @param username
     */
    public void registerSession(String id, String username) {
        if (id != null && username != null) {
            sessionCache.put(id, username);
            createMarker(null, username);
        }
    }

    /**
     * Remove session from session cache
     * @param id
     */
    public void unregisterSession(String id) {
        String username = getUserForSession(id);
        if (username != null) {
            removeMarker(username);
        }
        sessionCache.remove(id);
    }

    /**
     * Send specified map command to all users
     * @param command
     */
    private void sendMapCommand(MapCommand command) {
        logger.debug("MapCommand: " + command.getAction());
        messagingTemplate.convertAndSend(MAP_UPDATE_PATH, command);
    }

    /**
     * Type Info is not used. It's interesting to see it serialized into JSON.
     */
    @JsonTypeInfo(
            use = JsonTypeInfo.Id.MINIMAL_CLASS,
            include = JsonTypeInfo.As.PROPERTY,
            property = "type")
    public class UserMarker {


        private String username;
        private Double[] coords = new Double[2];

        public UserMarker(String username){
            this.setUsername(username);
        };

        public UserMarker(String username, List<Double> coords) {
            if (username != null) {
                this.username = username;
                this.setCoords(coords);
            }
        }

        public UserMarker(String username, Double lat, Double lng) {
            this.setUsername(username);
            this.coords[0] = lat;
            this.coords[1] = lng;
        }
        public void setCoords(List<Double> coords) {
            if (coords != null && coords.size() > 1) {
                this.coords[0] = coords.get(0);
                this.coords[1] = coords.get(1);
            }
        }

        public void setCoords(Double lat, Double lng) {
            this.coords[0] = lat;
            this.coords[1] = lng;
        }

        public Double[] getCoords() {
            return this.coords;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        @Override
        public String toString() {
            return new StringBuilder()
                    .append(username)
                    .append(":")
                    .append(this.coords[0])
                    .append(",")
                    .append(this.coords[1]).toString();
        }
    }
}
