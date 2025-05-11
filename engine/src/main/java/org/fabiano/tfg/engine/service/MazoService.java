package org.fabiano.tfg.engine.service;

import lombok.RequiredArgsConstructor;
import org.fabiano.tfg.engine.model.Carta;
import org.fabiano.tfg.engine.model.Palo;
import org.fabiano.tfg.engine.model.Partida;
import org.fabiano.tfg.engine.model.team.Equipo;
import org.fabiano.tfg.engine.model.team.Jugador;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MazoService {

    public List<Carta> crearMazo() {  // Cambiar de private a public
        List<Carta> mazo = new ArrayList<>();
        for (Palo palo : Palo.values()) {
            for (int valor = 1; valor <= 12; valor++) {
                if (valor != 8 && valor != 9) {
                    mazo.add(new Carta(palo, valor));
                }
            }
        }
        return mazo;
    }

    public void mezclarYRepartirCartas(Partida partida, List<Carta> mazo) {  // Agregar este método
        Collections.shuffle(mazo);
        int cartasPorJugador = 3;
        int cartaActual = 0;

        for (Equipo equipo : partida.getEquipos()) {
            for (Jugador jugador : equipo.getJugadores()) {
                jugador.setMano(new ArrayList<>());
                for (int i = 0; i < cartasPorJugador; i++) {
                    jugador.getMano().add(mazo.get(cartaActual++));
                }
            }
        }
    }
}