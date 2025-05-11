package org.fabiano.tfg.engine.model.team;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Equipo {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String nombre;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "equipo_jugadores",
            joinColumns = @JoinColumn(name = "equipo_id"),
            inverseJoinColumns = @JoinColumn(name = "jugador_id"),
            uniqueConstraints = {
                    @UniqueConstraint(columnNames = {"equipo_id", "jugador_id"})
            }
    )
    private List<Jugador> jugadores;
    private int puntaje;

    public Equipo(String nombre, List<Jugador> jugadores, int puntaje) {
        this.nombre = nombre;
        this.jugadores = jugadores;
        this.puntaje = puntaje;
    }
}