package org.fabiano.tfg.engine.repository;

import org.fabiano.tfg.engine.model.Carta;
import org.fabiano.tfg.engine.model.EstadoRonda;
import org.fabiano.tfg.engine.model.Palo;
import org.fabiano.tfg.engine.model.Partida;
import org.fabiano.tfg.engine.model.team.Equipo;
import org.fabiano.tfg.engine.model.team.Jugador;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PartidaRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PartidaRepository partidaRepository;

    @Autowired
    private CartaRepository cartaRepository;

    @Test
    void testPersistirYRecuperarPartida() {
        // Crear y persistir carta
        Carta carta = new Carta(Palo.ESPADA, 1);
        carta = entityManager.persist(carta);

        // Crear y persistir jugadores primero
        Jugador jugador1 = new Jugador("Jugador1", false, false, false, false, false,
                false, false, false, false, false, false, false, 0, new ArrayList<>());
        Jugador jugador2 = new Jugador("Jugador2", false, false, false, false, false,
                false, false, false, false, false, false, false, 0, new ArrayList<>());
        
        jugador1 = entityManager.persist(jugador1);
        jugador2 = entityManager.persist(jugador2);

        // Crear equipos con jugadores persistidos
        Equipo equipo1 = new Equipo("Equipo1", new ArrayList<>(), 0);
        Equipo equipo2 = new Equipo("Equipo2", new ArrayList<>(), 0);

        equipo1.getJugadores().add(jugador1);
        equipo2.getJugadores().add(jugador2);

        // Configurar partida
        Partida partida = new Partida();
        partida.setId(UUID.randomUUID());
        partida.setEquipos(Arrays.asList(equipo1, equipo2));
        partida.setEstadoRonda(EstadoRonda.EN_CURSO);
        partida.setPuntajeLimite(30);
        partida.setManoActual(1);
        partida.setRonda(1);
        partida.setVuelta(1);
        partida.setTrucoCantado(false);
        partida.setValorTruco(1);
        partida.setEnvidoCantado(false);
        partida.setValorEnvido(0);
        partida.setPuntosEnJuego(1);
        partida.setCartasJugadas(new ArrayList<>());
        partida.setGanadoresPorMano(new HashMap<>());
        partida.setPuntosPorEquipo(new HashMap<>());

        // Persistir y verificar
        Partida partidaGuardada = entityManager.persistAndFlush(partida);
        entityManager.clear();

        Optional<Partida> recuperada = partidaRepository.findById(partidaGuardada.getId());
        assertTrue(recuperada.isPresent());
        assertEquals(partidaGuardada.getId(), recuperada.get().getId());
        assertEquals(2, recuperada.get().getEquipos().size());
        assertEquals(30, recuperada.get().getPuntajeLimite());
    }

    @Test
    void testActualizarPartida() {
        // Crear partida inicial
        Partida partida = new Partida();
        partida.setId(UUID.randomUUID());
        partida.setEquipos(new ArrayList<>());
        partida.setEstadoRonda(EstadoRonda.EN_CURSO);

        // Persistir
        Partida inicial = entityManager.persistAndFlush(partida);
        entityManager.clear();

        // Modificar
        inicial.setEstadoRonda(EstadoRonda.FINALIZADA);
        inicial.setPuntosEnJuego(2);
        Partida actualizada = partidaRepository.save(inicial);
        entityManager.flush();
        entityManager.clear();

        // Verificar
        Optional<Partida> recuperada = partidaRepository.findById(actualizada.getId());
        assertTrue(recuperada.isPresent());
        assertEquals(EstadoRonda.FINALIZADA, recuperada.get().getEstadoRonda());
        assertEquals(2, recuperada.get().getPuntosEnJuego());
    }

    @Test
    void testEliminarPartida() {
        // Crear partida
        Partida partida = new Partida();
        partida.setId(UUID.randomUUID());
        partida.setEquipos(new ArrayList<>());

        // Persistir
        Partida guardada = entityManager.persistAndFlush(partida);
        entityManager.clear();

        // Eliminar
        partidaRepository.deleteById(guardada.getId());
        entityManager.flush();
        entityManager.clear();

        // Verificar
        Optional<Partida> recuperada = partidaRepository.findById(guardada.getId());
        assertFalse(recuperada.isPresent());
    }

    @Test
    void testPersistirPartidaConJugadas() {
        // Crear carta y partida
        Carta carta = new Carta(Palo.ESPADA, 1);
        carta = entityManager.persist(carta);

        Partida partida = new Partida();
        partida.setId(UUID.randomUUID());
        partida.setEquipos(new ArrayList<>());
        partida.setCartasJugadas(new ArrayList<>());

        // Agregar jugada
        partida.agregarCartaJugada("Jugador1", carta);

        // Persistir y verificar
        Partida guardada = entityManager.persistAndFlush(partida);
        entityManager.clear();

        Optional<Partida> recuperada = partidaRepository.findById(guardada.getId());
        assertTrue(recuperada.isPresent());
        assertEquals(1, recuperada.get().getCartasJugadas().size());
        assertEquals("Jugador1", recuperada.get().getCartasJugadas().get(0).getJugadorNombre());
    }

    @Test
    void testPersistirPartidaConPuntuaciones() {
        // Crear equipos
        Equipo equipo1 = new Equipo("Equipo1", new ArrayList<>(), 0);
        equipo1 = entityManager.persist(equipo1);

        // Crear partida
        Partida partida = new Partida();
        partida.setId(UUID.randomUUID());
        partida.setEquipos(Arrays.asList(equipo1));
        partida.setPuntosPorEquipo(new HashMap<>());
        partida.getPuntosPorEquipo().put(equipo1.getId(), 15);

        // Persistir y verificar
        Partida guardada = entityManager.persistAndFlush(partida);
        entityManager.clear();

        Optional<Partida> recuperada = partidaRepository.findById(guardada.getId());
        assertTrue(recuperada.isPresent());
        assertEquals(1, recuperada.get().getPuntosPorEquipo().size());
        assertEquals(15, recuperada.get().getPuntosPorEquipo().get(equipo1.getId()));
    }

    @Test
    void testPersistirPartidaConGanadoresPorMano() {
        // Crear equipo
        Equipo equipo1 = new Equipo("Equipo1", new ArrayList<>(), 0);
        equipo1 = entityManager.persist(equipo1);

        // Crear partida
        Partida partida = new Partida();
        partida.setId(UUID.randomUUID());
        partida.setEquipos(Arrays.asList(equipo1));
        partida.setGanadoresPorMano(new HashMap<>());
        partida.getGanadoresPorMano().put(1, equipo1.getId());

        // Persistir y verificar
        Partida guardada = entityManager.persistAndFlush(partida);
        entityManager.clear();

        Optional<Partida> recuperada = partidaRepository.findById(guardada.getId());
        assertTrue(recuperada.isPresent());
        assertEquals(1, recuperada.get().getGanadoresPorMano().size());
        assertEquals(equipo1.getId(), recuperada.get().getGanadoresPorMano().get(1));
    }
}