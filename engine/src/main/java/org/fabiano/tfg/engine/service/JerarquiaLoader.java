package org.fabiano.tfg.engine.service;


import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fabiano.tfg.engine.model.Carta;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.util.Map;

@Slf4j
@Service
public class JerarquiaLoader {

    @Getter
    private Map<String, Integer> jerarquia;

    @PostConstruct
    public void cargarJerarquia() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("data/jerarquia.yaml")) {
            if (input == null) {
                throw new IllegalStateException("No se encontró el archivo de jerarquía");
            }
            Yaml yaml = new Yaml();
            this.jerarquia = yaml.load(input);
            log.info("Jerarquía de cartas cargada exitosamente.");
        } catch (Exception e) {
            log.error("Error al cargar jerarquía de cartas", e);
        }
    }

    public int obtenerValor(Carta carta) {
        String clave = (carta.getPalo().name() + "_" + carta.getValor()).toUpperCase();

        // fallback para cartas no especiales
        if (!jerarquia.containsKey(clave)) {
            clave = "NUMERO_" + carta.getValor();
        }
        return jerarquia.getOrDefault(clave, 0);
    }
}
