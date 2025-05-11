package org.fabiano.tfg.engine.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Jugada {
    @Id
    @GeneratedValue
    private UUID id;

    private String jugadorNombre;

    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Carta carta;

    private int numeroVuelta;
    private int numeroRonda;

    public Jugada(String jugadorNombre, Carta carta, int numeroVuelta, int numeroRonda) {
        this.jugadorNombre = jugadorNombre;
        this.carta = carta;
        this.numeroVuelta = numeroVuelta;
        this.numeroRonda = numeroRonda;
    }

    public String getJugador() {
        return this.jugadorNombre;
    }
}