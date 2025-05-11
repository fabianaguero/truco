package org.fabiano.tfg.engine.service;

import org.fabiano.tfg.engine.model.Carta;
import org.fabiano.tfg.engine.model.Palo;
import org.fabiano.tfg.engine.model.Partida;
import org.fabiano.tfg.engine.model.team.Equipo;
import org.fabiano.tfg.engine.model.team.Jugador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ReglasTest {

    @Autowired
    private YamlRuleLoader yamlRuleLoader;

    @Autowired
    private JerarquiaLoader jerarquiaLoader;

    private Partida partida;
    private Jugador jugador1;
    private Jugador jugador2;

    @BeforeEach
    void setUp() {
        // Inicializar jugadores
        jugador1 = new Jugador("Jugador 1", false, false, false, false, false, false, false, false, false, false, false, false, 0, new ArrayList<>());
        jugador2 = new Jugador("Jugador 2", false, false, false, false, false, false, false, false, false, false, false, false, 0, new ArrayList<>());

        // Crear equipos
        Equipo equipo1 = new Equipo("Equipo 1", List.of(jugador1), 0);
        Equipo equipo2 = new Equipo("Equipo 2", List.of(jugador2), 0);

        // Inicializar partida
        partida = new Partida();
        partida.setEquipos(Arrays.asList(equipo1, equipo2));
        partida.setOrdenDeTurno(new LinkedList<>(Arrays.asList(jugador1, jugador2)));
        partida.setCartasJugadas(new ArrayList<>());
    }

    @Test
    void testReglaTruco() {
        // Cuando no se ha cantado truco
        yamlRuleLoader.ejecutarTodas(jugador1, partida);
        assertTrue(jugador1.isPuedeCantarTruco(), "El jugador debería poder cantar truco");

        // Después de cantar truco
        partida.setTrucoCantado(true);
        yamlRuleLoader.ejecutarTodas(jugador1, partida);
        assertFalse(jugador1.isPuedeCantarTruco(), "El jugador no debería poder cantar truco otra vez");
    }

    @Test
    void testReglaRetruco() {
        // Sin truco cantado
        yamlRuleLoader.ejecutarTodas(jugador1, partida);
        assertFalse(jugador1.isPuedeCantarRetruco(), "No debería poder cantar retruco sin truco previo");

        // Con truco cantado
        partida.setTrucoCantado(true);
        yamlRuleLoader.ejecutarTodas(jugador1, partida);
        assertTrue(jugador1.isPuedeCantarRetruco(), "Debería poder cantar retruco");
    }

    @Test
    void testReglaEnvido() {
        yamlRuleLoader.ejecutarTodas(jugador1, partida);
        assertTrue(jugador1.isPuedeCantarEnvido(), "Debería poder cantar envido inicialmente");

        partida.setEnvidoCantado(true);
        yamlRuleLoader.ejecutarTodas(jugador1, partida);
        assertFalse(jugador1.isPuedeCantarEnvido(), "No debería poder cantar envido después de cantarlo");
    }

    @Test
    void testReglaFlor() {
        // Configurar tres cartas del mismo palo
        jugador1.setMano(Arrays.asList(
                new Carta(Palo.ESPADA, 1),
                new Carta(Palo.ESPADA, 2),
                new Carta(Palo.ESPADA, 3)
        ));

        yamlRuleLoader.ejecutarTodas(jugador1, partida);
        assertTrue(jugador1.isPuedeCantarFlor(), "Debería poder cantar flor con tres cartas del mismo palo");

        // Configurar cartas de diferente palo
        jugador1.setMano(Arrays.asList(
                new Carta(Palo.ESPADA, 1),
                new Carta(Palo.BASTO, 2),
                new Carta(Palo.ORO, 3)
        ));

        yamlRuleLoader.ejecutarTodas(jugador1, partida);
        assertFalse(jugador1.isPuedeCantarFlor(), "No debería poder cantar flor con cartas de diferente palo");
    }

    @Test
    void testCalcularEnvido() {
        // Configurar dos cartas del mismo palo
        jugador1.setMano(Arrays.asList(
                new Carta(Palo.ESPADA, 7),
                new Carta(Palo.ESPADA, 6),
                new Carta(Palo.ORO, 1)
        ));

        yamlRuleLoader.ejecutarTodas(jugador1, partida);
        assertEquals(33, jugador1.getPuntosEnvido(), "Los puntos de envido deberían ser 33 (20 + 7 + 6)");
    }

    @Test
    void testQuieroNoQuiero() {
        partida.setTrucoCantado(true);
        yamlRuleLoader.ejecutarTodas(jugador2, partida);
        assertTrue(jugador2.isPuedeQuerer(), "Debería poder querer cuando hay un canto");
        assertTrue(jugador2.isPuedeNoQuerer(), "Debería poder no querer cuando hay un canto");
    }

    @Test
    void testIrseAlMazo() {
        yamlRuleLoader.ejecutarTodas(jugador1, partida);
        assertTrue(jugador1.isSeVaAlMazo(), "Debería poder irse al mazo en cualquier momento");
    }

    @Test
    void testContraflor() {
        partida.setFlorCantada(true);
        yamlRuleLoader.ejecutarTodas(jugador2, partida);
        assertTrue(jugador2.isPuedeCantarContraflor(), "Debería poder cantar contraflor cuando hay flor");
    }

    @Test
    void testContraflorAlResto() {
        partida.setContraflorCantada(true);
        yamlRuleLoader.ejecutarTodas(jugador1, partida);
        assertTrue(jugador1.isPuedeCantarContraflorAlResto(), "Debería poder cantar contraflor al resto cuando hay contraflor");
    }
}