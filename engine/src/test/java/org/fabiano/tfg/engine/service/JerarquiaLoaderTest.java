package org.fabiano.tfg.engine.service;


import org.fabiano.tfg.engine.model.Carta;
import org.fabiano.tfg.engine.model.Palo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class JerarquiaLoaderTest {

    @Autowired
    private JerarquiaLoader jerarquiaLoader;

    @BeforeEach
    void setUp() {
        jerarquiaLoader.cargarJerarquia();
    }

    @Test
    void testCargaJerarquia() {
        assertNotNull(jerarquiaLoader.getJerarquia(), "La jerarquía no debería ser nula");
        assertFalse(jerarquiaLoader.getJerarquia().isEmpty(), "La jerarquía no debería estar vacía");
    }

    @Test
    void testValoresCartasEspeciales() {
        // 1 de espadas es el más alto
        assertEquals(14, jerarquiaLoader.obtenerValor(new Carta(Palo.ESPADA, 1)), "1 de espadas debería valer 14");

        // 1 de bastos es el segundo más alto
        assertEquals(13, jerarquiaLoader.obtenerValor(new Carta(Palo.BASTO, 1)), "1 de bastos debería valer 13");

        // 7 de espadas es el tercero más alto
        assertEquals(12, jerarquiaLoader.obtenerValor(new Carta(Palo.ESPADA, 7)), "7 de espadas debería valer 12");

        // 7 de oro es el cuarto más alto
        assertEquals(11, jerarquiaLoader.obtenerValor(new Carta(Palo.ORO, 7)), "7 de oro debería valer 11");
    }

    @Test
    void testValoresCartasComunes() {
        // Los 3 valen lo mismo independiente del palo (10)
        assertEquals(10, jerarquiaLoader.obtenerValor(new Carta(Palo.ESPADA, 3)), "3 debería valer 10");
        assertEquals(10, jerarquiaLoader.obtenerValor(new Carta(Palo.BASTO, 3)), "3 debería valer 10");
        assertEquals(10, jerarquiaLoader.obtenerValor(new Carta(Palo.ORO, 3)), "3 debería valer 10");
        assertEquals(10, jerarquiaLoader.obtenerValor(new Carta(Palo.COPA, 3)), "3 debería valer 10");
    }

    @Test
    void testValoresCartasFallback() {
        // Cartas según su valor real en el juego
        assertEquals(1, jerarquiaLoader.obtenerValor(new Carta(Palo.COPA, 4)), "4 debería valer 1");
        assertEquals(2, jerarquiaLoader.obtenerValor(new Carta(Palo.ESPADA, 5)), "5 debería valer 2");
        assertEquals(3, jerarquiaLoader.obtenerValor(new Carta(Palo.ORO, 6)), "6 debería valer 3");
    }

    @Test
    void testOrdenJerarquico() {
        // Verificar que las cartas respetan el orden jerárquico
        Carta espadaUno = new Carta(Palo.ESPADA, 1);
        Carta bastoUno = new Carta(Palo.BASTO, 1);
        Carta espadaSiete = new Carta(Palo.ESPADA, 7);

        assertTrue(
                jerarquiaLoader.obtenerValor(espadaUno) > jerarquiaLoader.obtenerValor(bastoUno),
                "1 de espadas debe ser mayor que 1 de bastos"
        );

        assertTrue(
                jerarquiaLoader.obtenerValor(bastoUno) > jerarquiaLoader.obtenerValor(espadaSiete),
                "1 de bastos debe ser mayor que 7 de espadas"
        );
    }


}