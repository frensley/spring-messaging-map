package demo.message;

import demo.service.MapService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.ChannelInterceptorAdapter;
import org.springframework.stereotype.Component;

import java.security.Principal;

/**
 * Created by sfrensley on 4/16/14.
 */
@Component
public class MapChannelInterceptor extends ChannelInterceptorAdapter {

    protected Logger logger = LoggerFactory.getLogger(MapChannelInterceptor.class);

    @Autowired
    private MapService mapService;


    /**
     * Custom interceptor to detect disconnects and connects.
     * @param message
     * @param channel
     * @return
     */
    public Message preSend(Message message, MessageChannel channel) {

        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(message);
        SimpMessageType messageType = headers.getMessageType();
        logger.debug("Channel interceptor: {}", messageType);
        switch (messageType) {
            case DISCONNECT:
                if (mapService != null) {
                    String sessionId = headers.getSessionId();
                    String username = mapService.getUserForSession(sessionId);
                    logger.debug("Channel interceptor: user {} has disconnected with session id {}", username, sessionId);
                    mapService.unregisterSession(sessionId);
                }

            case CONNECT:
                logger.debug("Channel interceptor: {}", messageType);
                break;
            case CONNECT_ACK:
                if (mapService != null) {
                    Principal user = getConnectUser(message);
                    String sessionId = getConnectSessionId(message);
                    if (user != null) {
                        logger.debug("Channel interceptor: user {} has joined with session id {}", user.getName(), sessionId);
                        mapService.registerSession(sessionId, user.getName());
                    }
                }
                break;
            default:
                break;
        }
        return super.postReceive(message, channel);
    }

    private Principal getConnectUser(Message<?> message) {
        Principal user = getConnectHeaders(message).getUser();
        return user;
    }

    /**
     * Returns message session id
     *
     * @param message
     * @return
     */
    private String getConnectSessionId(Message<?> message) {
        return getConnectHeaders(message).getSessionId();
    }

    /**
     * Returns header accessor for connect message
     *
     * @param message
     * @return
     */
    private SimpMessageHeaderAccessor getConnectHeaders(Message<?> message) {
        MessageHeaders headers = message.getHeaders();
        Message connectMessage = (Message) headers.get(SimpMessageHeaderAccessor.CONNECT_MESSAGE_HEADER);
        return SimpMessageHeaderAccessor.wrap(connectMessage);
    }

    public MapService getMapService() {
        return mapService;
    }

    @Autowired
    public void setMapService(MapService mapService) {
        this.mapService = mapService;
    }
}
