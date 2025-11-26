package org.fabiano.tfg.engine.repository;


import org.fabiano.tfg.engine.model.Partida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PartidaRepository extends JpaRepository<Partida, String> {

    @Query("SELECT p FROM Partida p LEFT JOIN FETCH p.equipos e LEFT JOIN FETCH e.jugadores WHERE p.id = :id")
    Optional<Partida> findByIdWithEquipos(@Param("id") String id);

    @Query("SELECT p FROM Partida p WHERE p.estadoRonda = 'EN_CURSO'")
    List<Partida> findPartidasEnCurso();

    @Query("SELECT p FROM Partida p " +
            "LEFT JOIN p.equipos e " +
            "LEFT JOIN e.jugadores j " +
            "WHERE j.nombre = :nombreJugador")
    List<Partida> findPartidasByJugador(@Param("nombreJugador") String nombreJugador);

    @Query("SELECT COUNT(p) > 0 FROM Partida p " +
            "LEFT JOIN p.equipos e " +
            "LEFT JOIN e.jugadores j " +
            "WHERE p.id = :id AND j.nombre = :nombreJugador")
    boolean existsJugadorEnPartida(@Param("id") String id, @Param("nombreJugador") String nombreJugador);

    @Query("SELECT p FROM Partida p " +
            "WHERE p.manoActual > 0 " +
            "AND (p.trucoCantado = true OR p.envidoCantado = true)")
    List<Partida> findPartidasConCantos();

    @Query("SELECT DISTINCT p FROM Partida p " +
            "LEFT JOIN p.equipos e " +
            "WHERE e.puntaje >= :puntajeMinimo")
    List<Partida> findPartidasConPuntajeMinimo(@Param("puntajeMinimo") int puntajeMinimo);

    @Query(value = "SELECT * FROM partida p " +
            "WHERE p.version = (SELECT MAX(version) FROM partida WHERE id = p.id)",
            nativeQuery = true)
    List<Partida> findUltimasVersiones();

    // Método para limpiar partidas antiguas o abandonadas
    @Query("DELETE FROM Partida p WHERE p.version < :versionLimite")
    void limpiarPartidasAntiguas(@Param("versionLimite") Long versionLimite);

    // Métodos heredados de JpaRepository que ya están disponibles:
    // save(Partida)
    // findById(UUID)
    // findAll()
    // delete(Partida)
    // deleteById(UUID)
    // count()
    // existsById(UUID)
}