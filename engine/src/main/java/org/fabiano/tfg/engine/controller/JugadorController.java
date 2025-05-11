package org.fabiano.tfg.engine.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fabiano.tfg.engine.dto.CrearJugadorRequest;
import org.fabiano.tfg.engine.dto.JugadorResponse;
import org.fabiano.tfg.engine.model.team.Jugador;
import org.fabiano.tfg.engine.repository.JugadorRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/jugadores")
@RequiredArgsConstructor
public class JugadorController {
    private final JugadorRepository jugadorRepository;

    @PostMapping
    public ResponseEntity<JugadorResponse> crearJugador(@RequestBody CrearJugadorRequest request) {
        Jugador jugador = new Jugador(
                request.getNombre(),
                false, false, false, false, false,
                false, false, false, false, false,
                false, false, 0, new ArrayList<>()
        );
        jugador = jugadorRepository.save(jugador);
        log.info("Jugador creado: {}", jugador.getNombre());
        return ResponseEntity.ok(new JugadorResponse(jugador));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JugadorResponse> obtenerJugador(@PathVariable UUID id) {
        return jugadorRepository.findById(id)
                .map(jugador -> ResponseEntity.ok(new JugadorResponse(jugador)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<JugadorResponse>> listarJugadores() {
        List<JugadorResponse> jugadores = jugadorRepository.findAll().stream()
                .map(JugadorResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(jugadores);
    }

    @PutMapping("/{id}")
    public ResponseEntity<JugadorResponse> actualizarJugador(
            @PathVariable UUID id,
            @RequestBody CrearJugadorRequest request) {
        return jugadorRepository.findById(id)
                .map(jugador -> {
                    jugador.setNombre(request.getNombre());
                    jugador = jugadorRepository.save(jugador);
                    log.info("Jugador actualizado: {}", jugador.getNombre());
                    return ResponseEntity.ok(new JugadorResponse(jugador));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarJugador(@PathVariable UUID id) {
        if (jugadorRepository.existsById(id)) {
            jugadorRepository.deleteById(id);
            log.info("Jugador eliminado: {}", id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}