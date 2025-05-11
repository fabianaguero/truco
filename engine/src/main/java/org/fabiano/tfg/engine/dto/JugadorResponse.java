package org.fabiano.tfg.engine.dto;


import lombok.Data;
import org.fabiano.tfg.engine.model.team.Jugador;

import java.util.UUID;

@Data
public class JugadorResponse {
    private UUID id;
    private String nombre;

    public JugadorResponse(Jugador jugador) {
        this.id = jugador.getId();
        this.nombre = jugador.getNombre();
    }
}