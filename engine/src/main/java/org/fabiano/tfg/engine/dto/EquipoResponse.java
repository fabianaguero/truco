package org.fabiano.tfg.engine.dto;



import lombok.Data;
import org.fabiano.tfg.engine.model.team.Equipo;
import org.fabiano.tfg.engine.model.team.Jugador;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
public class EquipoResponse {
    private UUID id;
    private String nombre;
    private List<JugadorDTO> jugadores;
    private int puntaje;

    public EquipoResponse(Equipo equipo) {
        this.id = equipo.getId();
        this.nombre = equipo.getNombre();
        this.puntaje = equipo.getPuntaje();
        this.jugadores = equipo.getJugadores().stream()
            .map(JugadorDTO::new)
            .collect(Collectors.toList());
    }

    @Data
    public static class JugadorDTO {
        private String nombre;
        private int puntosEnvido;

        public JugadorDTO(Jugador jugador) {
            this.nombre = jugador.getNombre();
            this.puntosEnvido = jugador.getPuntosEnvido();
        }
    }
}