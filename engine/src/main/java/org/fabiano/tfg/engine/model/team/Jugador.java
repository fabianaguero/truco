package org.fabiano.tfg.engine.model.team;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fabiano.tfg.engine.model.Carta;

import java.util.List;
import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Jugador {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String nombre;

    // Estados del truco
    private boolean puedeCantarTruco;
    private boolean puedeCantarRetruco;
    private boolean puedeCantarValeCuatro;

    // Estados del envido
    private boolean puedeCantarEnvido;
    private boolean puedeCantarRealEnvido;
    private boolean puedeCantarFaltaEnvido;

    // Estados de la flor
    private boolean puedeCantarFlor;
    private boolean puedeCantarContraflor;
    private boolean puedeCantarContraflorAlResto;

    // Estados generales
    private boolean puedeQuerer;
    private boolean puedeNoQuerer;
    private boolean seVaAlMazo;

    private int puntosEnvido;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<Carta> mano;

    // Constructor para crear jugador con nombre y estados iniciales
    public Jugador(String nombre, boolean puedeCantarTruco, boolean puedeCantarRetruco,
                   boolean puedeCantarValeCuatro, boolean puedeCantarEnvido,
                   boolean puedeCantarRealEnvido, boolean puedeCantarFaltaEnvido,
                   boolean puedeCantarFlor, boolean puedeCantarContraflor,
                   boolean puedeCantarContraflorAlResto, boolean puedeQuerer,
                   boolean puedeNoQuerer, boolean seVaAlMazo, int puntosEnvido,
                   List<Carta> mano) {
        this.nombre = nombre;
        this.puedeCantarTruco = puedeCantarTruco;
        this.puedeCantarRetruco = puedeCantarRetruco;
        this.puedeCantarValeCuatro = puedeCantarValeCuatro;
        this.puedeCantarEnvido = puedeCantarEnvido;
        this.puedeCantarRealEnvido = puedeCantarRealEnvido;
        this.puedeCantarFaltaEnvido = puedeCantarFaltaEnvido;
        this.puedeCantarFlor = puedeCantarFlor;
        this.puedeCantarContraflor = puedeCantarContraflor;
        this.puedeCantarContraflorAlResto = puedeCantarContraflorAlResto;
        this.puedeQuerer = puedeQuerer;
        this.puedeNoQuerer = puedeNoQuerer;
        this.seVaAlMazo = seVaAlMazo;
        this.puntosEnvido = puntosEnvido;
        this.mano = mano;
    }
}