package org.fabiano.tfg.engine.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para la creación de una partida de Truco.
 * Permite elegir entre equipos aleatorios o definidos explícitamente.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CrearPartidaRequest {
    private boolean equiposAleatorios;
    private List<String> jugadores; // Si equiposAleatorios == true

    private List<EquipoDTO> equipos; // Si equiposAleatorios == false

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EquipoDTO {
        private String nombre;
        private List<String> jugadores;
    }
}
