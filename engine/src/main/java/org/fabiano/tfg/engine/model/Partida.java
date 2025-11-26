package org.fabiano.tfg.engine.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fabiano.tfg.engine.model.team.Equipo;
import org.fabiano.tfg.engine.model.team.Jugador;

import java.util.*;
import java.util.stream.Collectors;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Partida {
    @Id
    private String id;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Equipo> equipos;

    @Transient
    private Queue<Jugador> ordenDeTurno;

    // Índice del jugador actual en el turno (para persistencia)
    private int indiceTurnoActual = 0;

    private int puntajeLimite = 30;
    private int manoActual = 0;
    private int ronda = 1;
    private int vuelta = 1;

    @Enumerated(EnumType.STRING)
    private EstadoRonda estadoRonda;

    // Estados del truco
    private boolean trucoCantado;
    private boolean retrucoCantado;
    private boolean valeCuatroCantado;
    private int valorTruco = 1;

    // Estados del envido
    private boolean envidoCantado;
    private boolean realEnvidoCantado;
    private boolean faltaEnvidoCantado;
    private int valorEnvido = 0;

    // Estados de la flor
    private boolean florCantada;
    private boolean contraflorCantada;
    private boolean contraflorAlRestoCantada;

    private boolean alMazo;
    private boolean quiso;
    private boolean noQuiso;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "partida_id")
    private List<Jugada> cartasJugadas = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    private Map<Integer, UUID> ganadoresPorMano = new HashMap<>();

    @ManyToOne(fetch = FetchType.EAGER)
    private Equipo ganadorDeRonda;

    @ElementCollection(fetch = FetchType.EAGER)
    private Map<UUID, Integer> puntosPorEquipo = new HashMap<>();

    private int puntosEnJuego = 1;

    @Version
    private Long version;

    public void agregarCartaJugada(String jugadorNombre, Carta carta) {
        Jugada jugada = new Jugada(
                jugadorNombre,
                carta,
                this.vuelta,
                this.ronda
        );
        this.cartasJugadas.add(jugada);
    }

    public List<Jugada> getCartasJugadasEnVueltaActual() {
        if (this.cartasJugadas == null) {
            return new ArrayList<>();
        }
        return this.cartasJugadas.stream()
                .filter(j -> j.getNumeroVuelta() == this.vuelta &&
                        j.getNumeroRonda() == this.ronda)
                .collect(Collectors.toList());
    }

    public boolean esUltimaCartaDeLaVuelta() {
        return getCartasJugadasEnVueltaActual().size() == 4;
    }

    /**
     * Obtiene el número total de jugadores en la partida.
     */
    public int getTotalJugadores() {
        if (equipos == null) {
            return 0;
        }
        return equipos.stream()
                .mapToInt(e -> e.getJugadores() != null ? e.getJugadores().size() : 0)
                .sum();
    }

    /**
     * Obtiene el jugador que tiene el turno actual.
     */
    public Jugador getJugadorActual() {
        if (ordenDeTurno != null && !ordenDeTurno.isEmpty()) {
            return ordenDeTurno.peek();
        }
        return null;
    }

    /**
     * Verifica si es el turno del jugador especificado.
     * Compara por ID si está disponible, de lo contrario por nombre.
     */
    public boolean esTurnoDeJugador(Jugador jugador) {
        if (jugador == null) {
            return false;
        }
        Jugador actual = getJugadorActual();
        if (actual == null) {
            return false;
        }
        // Prefer ID comparison if both IDs are available
        if (actual.getId() != null && jugador.getId() != null) {
            return actual.getId().equals(jugador.getId());
        }
        // Fallback to name comparison
        return actual.getNombre() != null && 
               actual.getNombre().equalsIgnoreCase(jugador.getNombre());
    }
}