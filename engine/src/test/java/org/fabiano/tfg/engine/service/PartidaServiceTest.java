package org.fabiano.tfg.engine.service;

import org.fabiano.tfg.engine.dto.CrearPartidaRequest;
import org.fabiano.tfg.engine.model.Carta;
import org.fabiano.tfg.engine.model.EstadoRonda;
import org.fabiano.tfg.engine.model.Palo;
import org.fabiano.tfg.engine.model.Partida;
import org.fabiano.tfg.engine.model.team.Equipo;
import org.fabiano.tfg.engine.model.team.Jugador;
import org.fabiano.tfg.engine.repository.CartaRepository;
import org.fabiano.tfg.engine.repository.PartidaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PartidaServiceTest {

    @Mock
    private YamlRuleLoader ruleLoader;

    @Mock
    private PartidaRepository partidaRepository;

    @Mock
    private JerarquiaLoader jerarquiaLoader;

    @Mock
    private MazoService mazoService;

    @Mock
    private CartaRepository cartaRepository; // Agregar este mock

    private PartidaService partidaService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        partidaService = new PartidaService(
                mazoService,
                ruleLoader,
                partidaRepository,
                cartaRepository,    // Agregar cartaRepository aquí
                jerarquiaLoader
        );
        when(partidaRepository.save(any(Partida.class))).thenAnswer(i -> i.getArguments()[0]);
        when(cartaRepository.save(any(Carta.class))).thenAnswer(i -> i.getArguments()[0]);
    }

    @Test
    void testCrearPartidaConEquiposAleatorios() {
        CrearPartidaRequest request = new CrearPartidaRequest();
        request.setEquiposAleatorios(true);
        request.setJugadores(Arrays.asList("Jugador1", "Jugador2", "Jugador3", "Jugador4"));

        Partida partida = partidaService.crearPartida(request);

        assertNotNull(partida.getId());
        assertEquals(2, partida.getEquipos().size());
        assertEquals(30, partida.getPuntajeLimite());
        assertEquals(EstadoRonda.EN_CURSO, partida.getEstadoRonda());
        verify(partidaRepository, times(2)).save(any(Partida.class));
    }

    @Test
    void testCrearPartidaConEquiposDefinidos() {
        CrearPartidaRequest request = new CrearPartidaRequest();
        request.setEquiposAleatorios(false);
        List<CrearPartidaRequest.EquipoDTO> equipos = new ArrayList<>();

        CrearPartidaRequest.EquipoDTO equipo1 = new CrearPartidaRequest.EquipoDTO();
        equipo1.setNombre("Equipo1");
        equipo1.setJugadores(Arrays.asList("Jugador1", "Jugador2"));

        CrearPartidaRequest.EquipoDTO equipo2 = new CrearPartidaRequest.EquipoDTO();
        equipo2.setNombre("Equipo2");
        equipo2.setJugadores(Arrays.asList("Jugador3", "Jugador4"));

        equipos.add(equipo1);
        equipos.add(equipo2);
        request.setEquipos(equipos);

        Partida partida = partidaService.crearPartida(request);

        assertEquals(2, partida.getEquipos().size());
        assertEquals("Equipo1", partida.getEquipos().get(0).getNombre());
        assertEquals("Equipo2", partida.getEquipos().get(1).getNombre());
        verify(partidaRepository, times(2)).save(any(Partida.class));
    }

    @Test
    void testRegistrarJugada() {
        Partida partida = crearPartidaDePrueba();
        Jugador jugador = partida.getEquipos().get(0).getJugadores().get(0);
        Carta carta = new Carta(Palo.ESPADA, 1);
        jugador.setMano(new ArrayList<>(Collections.singletonList(carta)));

        partidaService.registrarJugada(partida, jugador, carta);

        assertEquals(1, partida.getCartasJugadas().size());
        assertTrue(jugador.getMano().isEmpty());
        verify(ruleLoader).ejecutarTodas(eq(jugador), eq(partida));
        verify(partidaRepository).save(partida);
    }

    @Test
    void testFinalizarManoGanada() {
        Partida partida = crearPartidaDePrueba();
        Equipo equipoGanador = partida.getEquipos().get(0);
        partida.setGanadorDeRonda(equipoGanador);
        partida.setPuntosEnJuego(2);
        UUID equipoId = UUID.randomUUID();
        equipoGanador.setId(equipoId);
        partida.setPuntosPorEquipo(new HashMap<>());

        partidaService.finalizarMano(partida);

        assertEquals(2, equipoGanador.getPuntaje());
        assertEquals(2, partida.getPuntosPorEquipo().get(equipoId));
        verify(partidaRepository, times(2)).save(partida);
    }

    @Test
    void testFinalizarPartida() {
        Partida partida = crearPartidaDePrueba();
        Equipo equipoGanador = partida.getEquipos().get(0);
        UUID equipoId = UUID.randomUUID();
        equipoGanador.setId(equipoId);
        equipoGanador.setPuntaje(29);  // Un punto menos del límite
        partida.setGanadorDeRonda(equipoGanador);
        partida.setPuntosEnJuego(2);   // Esto hará que supere el límite
        partida.setPuntosPorEquipo(new HashMap<>());

        // Asegurar que se inicialice el mapa de puntos
        partida.getPuntosPorEquipo().put(equipoId, 29);

        partidaService.finalizarMano(partida);

        assertEquals(EstadoRonda.FINALIZADA, partida.getEstadoRonda());
        assertEquals(31, equipoGanador.getPuntaje());
        assertEquals(31, partida.getPuntosPorEquipo().get(equipoId));
        verify(partidaRepository).save(partida);
    }

    private Partida crearPartidaDePrueba() {
        Partida partida = new Partida();
        partida.setId(UUID.randomUUID());
        partida.setEquipos(new ArrayList<>());
        partida.setCartasJugadas(new ArrayList<>());
        partida.setOrdenDeTurno(new LinkedList<>());
        partida.setPuntajeLimite(30);
        partida.setEstadoRonda(EstadoRonda.EN_CURSO);
        partida.setPuntosPorEquipo(new HashMap<>());

        Jugador jugador1 = new Jugador("Jugador1", false, false, false, false, false,
                false, false, false, false, false, false, false, 0, new ArrayList<>());
        Jugador jugador2 = new Jugador("Jugador2", false, false, false, false, false,
                false, false, false, false, false, false, false, 0, new ArrayList<>());

        Equipo equipo1 = new Equipo("Equipo1", new ArrayList<>(), 0);
        Equipo equipo2 = new Equipo("Equipo2", new ArrayList<>(), 0);

        equipo1.getJugadores().add(jugador1);
        equipo2.getJugadores().add(jugador2);

        partida.getEquipos().add(equipo1);
        partida.getEquipos().add(equipo2);

        return partida;
    }
}