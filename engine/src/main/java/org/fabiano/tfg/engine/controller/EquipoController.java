package org.fabiano.tfg.engine.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fabiano.tfg.engine.dto.CrearEquipoRequest;
import org.fabiano.tfg.engine.dto.EquipoResponse;
import org.fabiano.tfg.engine.model.team.Equipo;
import org.fabiano.tfg.engine.model.team.Jugador;
import org.fabiano.tfg.engine.repository.EquipoRepository;
import org.fabiano.tfg.engine.repository.JugadorRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/equipos")
@RequiredArgsConstructor
public class EquipoController {

    private final EquipoRepository equipoRepository;
    private final JugadorRepository jugadorRepository;

    @PostMapping
    public ResponseEntity<EquipoResponse> crearEquipo(@RequestBody CrearEquipoRequest request) {
        Equipo equipo = new Equipo(
                request.getNombre(),
                new ArrayList<>(),
                0
        );
        equipo = equipoRepository.save(equipo);
        log.info("Equipo creado: {}", equipo.getNombre());
        return ResponseEntity.ok(new EquipoResponse(equipo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EquipoResponse> obtenerEquipo(@PathVariable UUID id) {
        return equipoRepository.findById(id)
                .map(equipo -> ResponseEntity.ok(new EquipoResponse(equipo)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<EquipoResponse>> listarEquipos() {
        List<EquipoResponse> equipos = equipoRepository.findAll().stream()
                .map(EquipoResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(equipos);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EquipoResponse> actualizarEquipo(
            @PathVariable UUID id,
            @RequestBody CrearEquipoRequest request) {
        return equipoRepository.findById(id)
                .map(equipo -> {
                    equipo.setNombre(request.getNombre());
                    equipo = equipoRepository.save(equipo);
                    log.info("Equipo actualizado: {}", equipo.getNombre());
                    return ResponseEntity.ok(new EquipoResponse(equipo));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarEquipo(@PathVariable UUID id) {
        return equipoRepository.findById(id)
                .map(equipo -> {
                    equipoRepository.delete(equipo);
                    log.info("Equipo eliminado: {}", equipo.getNombre());
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{equipoId}/jugadores/{jugadorId}")
    @Operation(summary = "Asigna un jugador existente a un equipo")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Jugador asignado correctamente al equipo"),
            @ApiResponse(responseCode = "404", description = "Equipo o jugador no encontrado"),
            @ApiResponse(responseCode = "400", description = "Jugador ya existe en el equipo")
    })
    public ResponseEntity<EquipoResponse> asignarJugadorAEquipo(
            @Parameter(description = "ID del equipo", example = "eae55edd-3610-4915-9dbb-b93f9b126334")
            @PathVariable UUID equipoId,
            @Parameter(description = "ID del jugador", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID jugadorId) {
        Optional<Equipo> equipoOpt = equipoRepository.findById(equipoId);
        Optional<Jugador> jugadorOpt = jugadorRepository.findById(jugadorId);

        if (equipoOpt.isEmpty() || jugadorOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Equipo equipo = equipoOpt.get();
        Jugador jugador = jugadorOpt.get();

        if (existeJugadorEnEquipo(equipo, jugador.getNombre())) {
            return ResponseEntity.badRequest().build();
        }

        equipo.getJugadores().add(jugador);
        equipo = equipoRepository.save(equipo);
        log.info("Jugador {} asignado al equipo {}", jugador.getNombre(), equipo.getNombre());
        return ResponseEntity.ok(new EquipoResponse(equipo));
    }

    @DeleteMapping("/{equipoId}/jugadores/{jugadorId}")
    public ResponseEntity<EquipoResponse> removerJugadorDeEquipo(
            @PathVariable UUID equipoId,
            @PathVariable UUID jugadorId) {
        Optional<Equipo> equipoOpt = equipoRepository.findById(equipoId);
        if (equipoOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Equipo equipo = equipoOpt.get();
        boolean removido = equipo.getJugadores().removeIf(j -> j.getId().equals(jugadorId));

        if (!removido) {
            return ResponseEntity.notFound().build();
        }

        equipo = equipoRepository.save(equipo);
        log.info("Jugador removido del equipo {}", equipo.getNombre());
        return ResponseEntity.ok(new EquipoResponse(equipo));
    }

    private boolean existeJugadorEnEquipo(Equipo equipo, String nombreJugador) {
        return equipo.getJugadores().stream()
                .anyMatch(j -> j.getNombre().equals(nombreJugador));
    }
}