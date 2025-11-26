package org.fabiano.tfg.engine.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fabiano.tfg.engine.dto.CrearPartidaRequest;
import org.fabiano.tfg.engine.model.Carta;
import org.fabiano.tfg.engine.model.Partida;
import org.fabiano.tfg.engine.model.team.Jugador;
import org.fabiano.tfg.engine.repository.PartidaRepository;
import org.fabiano.tfg.engine.service.PartidaService;
import org.fabiano.tfg.engine.service.YamlRuleLoader;
import org.fabiano.tfg.engine.websocket.GameWebSocketHandler;
import org.fabiano.tfg.engine.websocket.WebSocketMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/partidas")
@RequiredArgsConstructor
public class PartidaController {

    private final PartidaService partidaService;
    private final YamlRuleLoader yamlRuleLoader;
    private final PartidaRepository partidaRepository;
    private final GameWebSocketHandler webSocketHandler;

    @PostMapping
    public ResponseEntity<String> crearPartida(@RequestBody CrearPartidaRequest request) {
        if (request.isEquiposAleatorios() && request.getJugadores().size() % 2 != 0) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try {
            Partida partida = partidaService.crearPartida(request);
            log.info("Partida creada con ID: {}", partida.getId());
            return new ResponseEntity<>(partida.getId().toString(), HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Partida> obtenerPartida(@PathVariable UUID id) {
        Optional<Partida> partidaOpt = partidaRepository.findById(id);
        if (partidaOpt.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Partida partida = partidaOpt.get();
        // Reconstruir el orden de turno si es necesario
        partidaService.reconstruirOrdenDeTurno(partida);
        return new ResponseEntity<>(partida, HttpStatus.OK);
    }

    @PostMapping("/{id}/cantar/truco")
    public ResponseEntity<String> cantarTruco(@PathVariable UUID id, @RequestParam String jugadorNombre) {
        Optional<Partida> partidaOpt = partidaRepository.findById(id);
        if (partidaOpt.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Partida partida = partidaOpt.get();
        partidaService.reconstruirOrdenDeTurno(partida);
        
        Jugador jugador = encontrarJugador(partida, jugadorNombre);
        if (jugador == null) {
            return new ResponseEntity<>("Jugador no encontrado", HttpStatus.BAD_REQUEST);
        }

        yamlRuleLoader.ejecutarTodas(jugador, partida);
        if (!jugador.isPuedeCantarTruco()) {
            return new ResponseEntity<>("No puede cantar truco en este momento", HttpStatus.BAD_REQUEST);
        }

        partida.setTrucoCantado(true);
        partida.setValorTruco(2);
        partidaService.avanzarTurno(partida);
        partidaRepository.save(partida);
        log.info("Jugador {} cantó truco en partida {}", jugador.getNombre(), partida.getId());
        
        // Broadcast WebSocket update
        broadcastGameUpdate(partida, WebSocketMessage.MessageType.TRUCO_CANTADO, jugador.getNombre());
        
        return new ResponseEntity<>(jugador.getNombre() + " cantó truco", HttpStatus.OK);
    }

    @PostMapping("/{id}/cantar/envido")
    public ResponseEntity<String> cantarEnvido(@PathVariable UUID id, @RequestParam String jugadorNombre) {
        Optional<Partida> partidaOpt = partidaRepository.findById(id);
        if (partidaOpt.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Partida partida = partidaOpt.get();
        partidaService.reconstruirOrdenDeTurno(partida);
        
        Jugador jugador = encontrarJugador(partida, jugadorNombre);
        if (jugador == null) {
            return new ResponseEntity<>("Jugador no encontrado", HttpStatus.BAD_REQUEST);
        }

        yamlRuleLoader.ejecutarTodas(jugador, partida);
        if (!jugador.isPuedeCantarEnvido()) {
            return new ResponseEntity<>("No puede cantar envido en este momento", HttpStatus.BAD_REQUEST);
        }

        partida.setEnvidoCantado(true);
        partida.setValorEnvido(2);
        partidaService.avanzarTurno(partida);
        partidaRepository.save(partida);
        log.info("Jugador {} cantó envido en partida {}", jugador.getNombre(), partida.getId());
        
        // Broadcast WebSocket update
        broadcastGameUpdate(partida, WebSocketMessage.MessageType.ENVIDO_CANTADO, jugador.getNombre());
        
        return new ResponseEntity<>(jugador.getNombre() + " cantó envido", HttpStatus.OK);
    }

    @PostMapping("/{id}/jugar")
    public ResponseEntity<String> jugarCarta(
            @PathVariable UUID id,
            @RequestParam String jugadorNombre,
            @RequestParam int indiceCarta) {
        Optional<Partida> partidaOpt = partidaRepository.findById(id);
        if (partidaOpt.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Partida partida = partidaOpt.get();
        // Reconstruir el orden de turno si es necesario
        partidaService.reconstruirOrdenDeTurno(partida);

        Jugador jugador = encontrarJugador(partida, jugadorNombre);
        if (jugador == null || indiceCarta < 0 || indiceCarta >= jugador.getMano().size()) {
            return new ResponseEntity<>("Jugador o carta inválida", HttpStatus.BAD_REQUEST);
        }

        // Validar que es el turno del jugador
        if (!partida.esTurnoDeJugador(jugador)) {
            return new ResponseEntity<>("No es tu turno. Turno actual: " + 
                    partida.getJugadorActual().getNombre(), HttpStatus.BAD_REQUEST);
        }

        try {
            yamlRuleLoader.ejecutarTodas(jugador, partida);
            Carta carta = jugador.getMano().get(indiceCarta);
            partidaService.registrarJugada(partida, jugador, carta);
            partidaRepository.save(partida);
            log.info("Jugador {} jugó carta {} en partida {}",
                    jugador.getNombre(), carta, partida.getId());
            
            // Broadcast WebSocket update
            Map<String, Object> cartaJugadaInfo = new HashMap<>();
            cartaJugadaInfo.put("jugador", jugador.getNombre());
            cartaJugadaInfo.put("carta", carta);
            broadcastGameUpdate(partida, WebSocketMessage.MessageType.CARTA_JUGADA, cartaJugadaInfo);
            
            return new ResponseEntity<>(jugador.getNombre() + " jugó: " + carta, HttpStatus.OK);
        } catch (IllegalStateException e) {
            log.warn("Error al jugar carta: {}", e.getMessage());
            return new ResponseEntity<>("No se puede realizar esta acción en este momento", HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/{id}/querer")
    public ResponseEntity<String> querer(@PathVariable UUID id, @RequestParam String jugadorNombre) {
        Optional<Partida> partidaOpt = partidaRepository.findById(id);
        if (partidaOpt.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Partida partida = partidaOpt.get();
        partidaService.reconstruirOrdenDeTurno(partida);
        
        Jugador jugador = encontrarJugador(partida, jugadorNombre);
        if (jugador == null) {
            return new ResponseEntity<>("Jugador no encontrado", HttpStatus.BAD_REQUEST);
        }

        yamlRuleLoader.ejecutarTodas(jugador, partida);
        if (!jugador.isPuedeQuerer()) {
            return new ResponseEntity<>("No puede querer en este momento", HttpStatus.BAD_REQUEST);
        }

        partida.setQuiso(true);
        partidaService.avanzarTurno(partida);
        partidaRepository.save(partida);
        log.info("Jugador {} quiso en partida {}", jugador.getNombre(), partida.getId());
        
        // Broadcast WebSocket update
        broadcastGameUpdate(partida, WebSocketMessage.MessageType.QUISO, jugador.getNombre());
        
        return new ResponseEntity<>(jugador.getNombre() + " quiso", HttpStatus.OK);
    }

    @PostMapping("/{id}/no-querer")
    public ResponseEntity<String> noQuerer(@PathVariable UUID id, @RequestParam String jugadorNombre) {
        Optional<Partida> partidaOpt = partidaRepository.findById(id);
        if (partidaOpt.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Partida partida = partidaOpt.get();
        partidaService.reconstruirOrdenDeTurno(partida);
        
        Jugador jugador = encontrarJugador(partida, jugadorNombre);
        if (jugador == null) {
            return new ResponseEntity<>("Jugador no encontrado", HttpStatus.BAD_REQUEST);
        }

        yamlRuleLoader.ejecutarTodas(jugador, partida);
        if (!jugador.isPuedeNoQuerer()) {
            return new ResponseEntity<>("No puede no querer en este momento", HttpStatus.BAD_REQUEST);
        }

        partida.setNoQuiso(true);
        partidaService.finalizarMano(partida);
        partidaRepository.save(partida);
        log.info("Jugador {} no quiso en partida {}", jugador.getNombre(), partida.getId());
        
        // Broadcast WebSocket update
        broadcastGameUpdate(partida, WebSocketMessage.MessageType.NO_QUISO, jugador.getNombre());
        
        return new ResponseEntity<>(jugador.getNombre() + " no quiso", HttpStatus.OK);
    }

    @PostMapping("/{id}/mazo")
    public ResponseEntity<String> irseAlMazo(@PathVariable UUID id, @RequestParam String jugadorNombre) {
        Optional<Partida> partidaOpt = partidaRepository.findById(id);
        if (partidaOpt.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Partida partida = partidaOpt.get();
        partidaService.reconstruirOrdenDeTurno(partida);
        
        Jugador jugador = encontrarJugador(partida, jugadorNombre);
        if (jugador == null) {
            return new ResponseEntity<>("Jugador no encontrado", HttpStatus.BAD_REQUEST);
        }

        yamlRuleLoader.ejecutarTodas(jugador, partida);
        if (!jugador.isSeVaAlMazo()) {
            return new ResponseEntity<>("No puede irse al mazo en este momento", HttpStatus.BAD_REQUEST);
        }

        partida.setAlMazo(true);
        partidaService.finalizarMano(partida);
        partidaRepository.save(partida);
        log.info("Jugador {} se fue al mazo en partida {}", jugador.getNombre(), partida.getId());
        
        // Broadcast WebSocket update
        broadcastGameUpdate(partida, WebSocketMessage.MessageType.AL_MAZO, jugador.getNombre());
        
        return new ResponseEntity<>(jugador.getNombre() + " se fue al mazo", HttpStatus.OK);
    }

    @GetMapping("/{id}/mano")
    public ResponseEntity<?> obtenerManoJugador(
            @PathVariable UUID id,
            @RequestParam String jugadorNombre) {
        Optional<Partida> partidaOpt = partidaRepository.findById(id);
        if (partidaOpt.isEmpty()) {
            return new ResponseEntity<>("Partida no encontrada", HttpStatus.NOT_FOUND);
        }

        Partida partida = partidaOpt.get();
        partidaService.reconstruirOrdenDeTurno(partida);
        
        Jugador jugador = encontrarJugador(partida, jugadorNombre);
        if (jugador == null) {
            return new ResponseEntity<>("Jugador no encontrado", HttpStatus.BAD_REQUEST);
        }

        // Creamos un DTO con solo la información necesaria
        Map<String, Object> response = new HashMap<>();
        response.put("jugador", jugador.getNombre());
        response.put("cartas", jugador.getMano());
        Jugador jugadorActual = partida.getJugadorActual();
        response.put("turnoActual", jugadorActual != null ? jugadorActual.getNombre() : null);
        response.put("esMiTurno", partida.esTurnoDeJugador(jugador));
        response.put("estadoRonda", partida.getEstadoRonda());
        response.put("cartasJugadas", partida.getCartasJugadas());
        response.put("puntosEquipo1", partida.getEquipos().get(0).getPuntaje());
        response.put("puntosEquipo2", partida.getEquipos().get(1).getPuntaje());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private Jugador encontrarJugador(Partida partida, String nombre) {
        return partida.getOrdenDeTurno().stream()
                .filter(j -> j.getNombre().equalsIgnoreCase(nombre))
                .findFirst()
                .orElse(null);
    }
    
    private void broadcastGameUpdate(Partida partida, WebSocketMessage.MessageType type, Object payload) {
        WebSocketMessage message = WebSocketMessage.builder()
                .type(type)
                .payload(payload)
                .partidaId(partida.getId().toString())
                .build();
        webSocketHandler.broadcastToPartida(partida.getId().toString(), message);
    }
}