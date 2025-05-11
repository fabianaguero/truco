package org.fabiano.tfg.engine.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fabiano.tfg.engine.dto.CrearPartidaRequest;
import org.fabiano.tfg.engine.model.Carta;
import org.fabiano.tfg.engine.model.EstadoRonda;
import org.fabiano.tfg.engine.model.Jugada;
import org.fabiano.tfg.engine.model.Partida;
import org.fabiano.tfg.engine.model.team.Equipo;
import org.fabiano.tfg.engine.model.team.Jugador;
import org.fabiano.tfg.engine.repository.CartaRepository;
import org.fabiano.tfg.engine.repository.PartidaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartidaService {

    private final MazoService mazoService;
    private final YamlRuleLoader ruleLoader;
    private final PartidaRepository partidaRepository;
    private final CartaRepository cartaRepository;
    private final JerarquiaLoader jerarquiaLoader;

    @Transactional
    public Partida crearPartida(CrearPartidaRequest request) {
        List<Equipo> equipos = crearEquipos(request);
        Partida partida = new Partida();
        partida.setId(UUID.randomUUID());
        partida.setEquipos(equipos);
        partida.setPuntajeLimite(30);
        partida.setCartasJugadas(new ArrayList<>());
        partida.setGanadoresPorMano(new HashMap<>());
        partida.setPuntosPorEquipo(new HashMap<>());
        partida.setEstadoRonda(EstadoRonda.EN_CURSO);

        iniciarNuevaMano(partida);
        return partidaRepository.save(partida);
    }

    private List<Equipo> crearEquipos(CrearPartidaRequest request) {
        List<Equipo> equipos = new ArrayList<>();

        if (request.isEquiposAleatorios()) {
            List<String> jugadores = new ArrayList<>(request.getJugadores());
            Collections.shuffle(jugadores);
            for (int i = 0; i < jugadores.size(); i += 2) {
                String nombreEquipo = "Equipo-" + (i / 2 + 1);
                List<Jugador> miembros = crearJugadores(jugadores.subList(i, i + 2));
                equipos.add(new Equipo(nombreEquipo, miembros, 0));
            }
        } else {
            for (CrearPartidaRequest.EquipoDTO dto : request.getEquipos()) {
                List<Jugador> miembros = crearJugadores(dto.getJugadores());
                equipos.add(new Equipo(dto.getNombre(), miembros, 0));
            }
        }
        return equipos;
    }

    private List<Jugador> crearJugadores(List<String> nombres) {
        return nombres.stream()
                .map(nombre -> new Jugador(
                        nombre,
                        false, false, false, false, false, false,
                        false, false, false, false, false, false,
                        0, new ArrayList<>()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<Partida> obtenerPartida(UUID id) {
        return partidaRepository.findById(id);
    }

    @Transactional
    public void iniciarNuevaMano(Partida partida) {
        log.info("Iniciando nueva mano...");
        reiniciarEstadosMano(partida);

        // Guardar las cartas del mazo antes de repartir
        List<Carta> mazoNuevo = mazoService.crearMazo().stream()
                .map(cartaRepository::save)
                .toList();

        mazoService.mezclarYRepartirCartas(partida, mazoNuevo);
        ordenarTurno(partida);

        for (Jugador jugador : obtenerTodosLosJugadores(partida)) {
            ruleLoader.ejecutarTodas(jugador, partida);
        }

        partidaRepository.save(partida);
    }

    private void reiniciarEstadosMano(Partida partida) {
        partida.setManoActual(partida.getManoActual() + 1);
        partida.setRonda(1);
        partida.setVuelta(1);
        partida.setEstadoRonda(EstadoRonda.EN_CURSO);

        partida.setTrucoCantado(false);
        partida.setRetrucoCantado(false);
        partida.setValeCuatroCantado(false);
        partida.setEnvidoCantado(false);
        partida.setRealEnvidoCantado(false);
        partida.setFaltaEnvidoCantado(false);
        partida.setFlorCantada(false);
        partida.setContraflorCantada(false);
        partida.setContraflorAlRestoCantada(false);
        partida.setQuiso(false);
        partida.setNoQuiso(false);
        partida.setAlMazo(false);

        partida.setCartasJugadas(new ArrayList<>());
        partida.setGanadorDeRonda(null);
        partida.setPuntosEnJuego(1);
        partida.setValorTruco(1);
        partida.setValorEnvido(0);
    }

    @Transactional
    public void registrarJugada(Partida partida, Jugador jugador, Carta carta) {
        // Guardar la carta primero
        carta = cartaRepository.save(carta);

        Jugada jugada = new Jugada(
                jugador.getNombre(),
                carta,
                partida.getVuelta(),
                partida.getRonda()
        );

        partida.getCartasJugadas().add(jugada);
        jugador.getMano().remove(carta);
        log.info("{} jugó {} de {}", jugador.getNombre(), carta.getValor(), carta.getPalo());

        ruleLoader.ejecutarTodas(jugador, partida);

        if (partida.getCartasJugadas().size() % partida.getEquipos().size() == 0) {
            resolverRonda(partida);
        }

        partidaRepository.save(partida);
    }

    @Transactional
    public void avanzarTurno(Partida partida) {
        Jugador jugadorActual = partida.getOrdenDeTurno().poll();
        partida.getOrdenDeTurno().offer(jugadorActual);

        Jugador siguienteJugador = partida.getOrdenDeTurno().peek();
        ruleLoader.ejecutarTodas(siguienteJugador, partida);

        partidaRepository.save(partida);
    }

    private void resolverRonda(Partida partida) {
        List<Jugada> jugadasRonda = obtenerJugadasUltimaRonda(partida);
        if (jugadasRonda.isEmpty()) return;

        Jugada jugadaGanadora = jugadasRonda.stream()
                .max(Comparator.comparingInt(j -> jerarquiaLoader.obtenerValor(j.getCarta())))
                .orElse(null);

        if (jugadaGanadora != null) {
            String nombreJugador = jugadaGanadora.getJugador();
            Jugador jugador = obtenerTodosLosJugadores(partida).stream()
                    .filter(j -> j.getNombre().equals(nombreJugador))
                    .findFirst()
                    .orElse(null);

            if (jugador != null) {
                Equipo equipoGanador = encontrarEquipoDeJugador(partida, jugador);
                partida.setGanadorDeRonda(equipoGanador);
                log.info("Ganador de la ronda: {} con {}",
                        nombreJugador,
                        jugadaGanadora.getCarta());
            }
        }

        for (Jugador jugador : obtenerTodosLosJugadores(partida)) {
            ruleLoader.ejecutarTodas(jugador, partida);
        }

        if (partida.getGanadorDeRonda() != null) {
            asignarPuntos(partida);
        }
    }

    private void asignarPuntos(Partida partida) {
        Equipo equipoGanador = partida.getGanadorDeRonda();
        int puntosActuales = partida.getPuntosPorEquipo().getOrDefault(equipoGanador.getId(), 0);
        int nuevosPuntos = puntosActuales + partida.getPuntosEnJuego();

        partida.getPuntosPorEquipo().put(equipoGanador.getId(), nuevosPuntos);
        equipoGanador.setPuntaje(nuevosPuntos);

        log.info("Equipo {} ganó {} puntos. Total: {}",
                equipoGanador.getNombre(),
                partida.getPuntosEnJuego(),
                nuevosPuntos);
    }

    private List<Jugador> obtenerTodosLosJugadores(Partida partida) {
        return partida.getEquipos().stream()
                .flatMap(e -> e.getJugadores().stream())
                .toList();
    }

    private void ordenarTurno(Partida partida) {
        Queue<Jugador> orden = new LinkedList<>();
        for (int i = 0; i < 2; i++) {
            for (Equipo equipo : partida.getEquipos()) {
                if (equipo.getJugadores().size() > i) {
                    orden.add(equipo.getJugadores().get(i));
                }
            }
        }
        partida.setOrdenDeTurno(orden);
    }

    @Transactional
    public void finalizarMano(Partida partida) {
        log.info("Finalizando mano...");

        for (Jugador jugador : obtenerTodosLosJugadores(partida)) {
            ruleLoader.ejecutarTodas(jugador, partida);
        }

        if (partida.getGanadorDeRonda() != null) {
            asignarPuntos(partida);

            if (verificarFinPartida(partida)) {
                partida.setEstadoRonda(EstadoRonda.FINALIZADA);
            } else {
                iniciarNuevaMano(partida);
            }
        }

        partidaRepository.save(partida);
    }

    private boolean verificarFinPartida(Partida partida) {
        return partida.getEquipos().stream()
                .anyMatch(e -> e.getPuntaje() >= partida.getPuntajeLimite());
    }

    private List<Jugada> obtenerJugadasUltimaRonda(Partida partida) {
        int inicio = partida.getCartasJugadas().size() - partida.getEquipos().size() * 2;
        if (inicio < 0) inicio = 0;
        return partida.getCartasJugadas().subList(inicio, partida.getCartasJugadas().size());
    }

    private Equipo encontrarEquipoDeJugador(Partida partida, Jugador jugador) {
        return partida.getEquipos().stream()
                .filter(e -> e.getJugadores().contains(jugador))
                .findFirst()
                .orElse(null);
    }
}