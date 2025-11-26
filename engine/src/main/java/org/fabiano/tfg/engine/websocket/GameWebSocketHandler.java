package org.fabiano.tfg.engine.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    // Map partidaId -> Set of connected sessions
    private final Map<String, Set<WebSocketSession>> partidaSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Pattern to extract partidaId from the WebSocket path
    private static final Pattern PARTIDA_PATH_PATTERN = Pattern.compile("/ws/partida/([a-fA-F0-9-]+)");

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String partidaId = extractPartidaId(session);
        if (partidaId != null) {
            partidaSessions.computeIfAbsent(partidaId, k -> new CopyOnWriteArraySet<>()).add(session);
            log.info("WebSocket connected for partida {}: session {}", partidaId, session.getId());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String partidaId = extractPartidaId(session);
        if (partidaId != null) {
            Set<WebSocketSession> sessions = partidaSessions.get(partidaId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    partidaSessions.remove(partidaId);
                }
            }
            log.info("WebSocket disconnected for partida {}: session {}", partidaId, session.getId());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Handle incoming messages if needed
        log.debug("Received message: {}", message.getPayload());
    }

    /**
     * Broadcast a message to all sessions connected to a specific partida.
     */
    public void broadcastToPartida(String partidaId, WebSocketMessage message) {
        Set<WebSocketSession> sessions = partidaSessions.get(partidaId);
        if (sessions != null && !sessions.isEmpty()) {
            try {
                String jsonMessage = objectMapper.writeValueAsString(message);
                TextMessage textMessage = new TextMessage(jsonMessage);
                
                for (WebSocketSession session : sessions) {
                    if (session.isOpen()) {
                        try {
                            session.sendMessage(textMessage);
                        } catch (IOException e) {
                            log.error("Error sending message to session {}: {}", session.getId(), e.getMessage());
                        }
                    }
                }
                log.debug("Broadcast message to {} sessions for partida {}", sessions.size(), partidaId);
            } catch (Exception e) {
                log.error("Error serializing message: {}", e.getMessage());
            }
        }
    }

    private String extractPartidaId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) {
            return null;
        }
        
        String path = uri.getPath();
        if (path == null) {
            return null;
        }
        
        Matcher matcher = PARTIDA_PATH_PATTERN.matcher(path);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }
}
