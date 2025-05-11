package org.fabiano.tfg.engine.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Carta {
    @Id
    @GeneratedValue
    private Long id;

    @Enumerated(EnumType.STRING)
    private Palo palo;

    @Column(name = "valor")
    private int valor;

    // Constructor para crear cartas sin ID (para uso en el juego)
    public Carta(Palo palo, int valor) {
        this.palo = palo;
        this.valor = valor;
    }

    @Override
    public String toString() {
        return valor + " de " + palo;
    }
}