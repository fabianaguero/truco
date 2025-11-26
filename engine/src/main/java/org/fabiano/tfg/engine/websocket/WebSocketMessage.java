package org.fabiano.tfg.engine.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WebSocketMessage {
    
    public enum MessageType {
        PARTIDA_UPDATE,
        CARTA_JUGADA,
        TRUCO_CANTADO,
        ENVIDO_CANTADO,
        TURNO_CAMBIO,
        QUISO,
        NO_QUISO,
        AL_MAZO
    }
    
    private MessageType type;
    private Object payload;
    private String partidaId;
}
