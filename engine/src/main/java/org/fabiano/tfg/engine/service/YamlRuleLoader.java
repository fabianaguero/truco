package org.fabiano.tfg.engine.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fabiano.tfg.engine.model.Partida;
import org.fabiano.tfg.engine.model.team.Jugador;
import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.Rules;
import org.jeasy.rules.api.RulesEngine;
import org.jeasy.rules.core.DefaultRulesEngine;
import org.jeasy.rules.mvel.MVELRuleFactory;
import org.jeasy.rules.support.reader.YamlRuleDefinitionReader;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

@Slf4j
@Service
@RequiredArgsConstructor
public class YamlRuleLoader {

    private static final String RULES_PATH = "rules/";
    private final JerarquiaLoader jerarquiaLoader;

    public void ejecutarTodas(Jugador jugador, Partida partida) {
        try {
            Rules rules = new Rules();
            MVELRuleFactory ruleFactory = new MVELRuleFactory(new YamlRuleDefinitionReader());
            ClassLoader classLoader = getClass().getClassLoader();
            URL rulesDirURL = classLoader.getResource(RULES_PATH);

            if (rulesDirURL == null) {
                log.error("No se encontró el directorio de reglas: {}", RULES_PATH);
                return;
            }

            File rulesDir = new File(rulesDirURL.toURI());
            File[] ruleFiles = rulesDir.listFiles((dir, name) -> name.endsWith(".yml"));

            if (ruleFiles == null || ruleFiles.length == 0) {
                log.warn("No se encontraron archivos de reglas en el directorio: {}", RULES_PATH);
                return;
            }

            for (File file : ruleFiles) {
                try (InputStream stream = classLoader.getResourceAsStream(RULES_PATH + file.getName())) {
                    if (stream != null) {
                        try (Reader reader = new InputStreamReader(stream)) {
                            rules.register(ruleFactory.createRule(reader));
                            log.info("Regla cargada exitosamente: {}", file.getName());
                        }
                    } else {
                        log.error("No se pudo abrir el archivo de regla: {}", file.getName());
                    }
                } catch (Exception e) {
                    log.error("Error al procesar el archivo de regla: {}", file.getName(), e);
                }
            }

            if (rules.isEmpty()) {
                log.error("No se cargaron reglas válidas. Verifica los archivos YAML.");
                return;
            }

            // Construir facts para las reglas
            Facts facts = new Facts();
            facts.put("jugador", jugador);
            facts.put("partida", partida);
            facts.put("ordenDeTurno", partida.getOrdenDeTurno());
            facts.put("cartasJugadas", partida.getCartasJugadas());
            facts.put("jerarquia", jerarquiaLoader.getJerarquia());

            RulesEngine engine = new DefaultRulesEngine();
            engine.fire(rules, facts);

        } catch (Exception e) {
            log.error("Error general al cargar y ejecutar reglas YAML", e);
        }
    }
}