package org.fabiano.tfg.engine.model;

import lombok.Data;
import org.fabiano.tfg.engine.model.team.Equipo;
import org.fabiano.tfg.engine.model.team.Jugador;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Estado simplificado de la partida para evaluar reglas.
 */
@Data
public class EstadoPartida {
    private String id;
    private List<EquipoEstadoDTO> equipos;
    private String jugadorActual;
    private int ronda;
    private int vuelta;
    private EstadoRonda estado;

    public EstadoPartida(Partida partida) {
        this.id = partida.getId();
        this.equipos = partida.getEquipos().stream()
                .map(EquipoEstadoDTO::new)
                .collect(Collectors.toList());
        this.jugadorActual = partida.getOrdenDeTurno().peek().getNombre();
        this.ronda = partida.getRonda();
        this.vuelta = partida.getVuelta();
        this.estado = partida.getEstadoRonda();
    }

    @Data
    public static class EquipoEstadoDTO {
        private String nombre;
        private List<String> jugadores;
        private int puntaje;

        public EquipoEstadoDTO(Equipo equipo) {
            this.nombre = equipo.getNombre();
            this.jugadores = equipo.getJugadores().stream()
                    .map(Jugador::getNombre)
                    .collect(Collectors.toList());
            this.puntaje = equipo.getPuntaje();
        }
    }
}